package com.example.gemerador.Gestion;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GestionTickets extends AppCompatActivity implements TicketAdapter.TicketActionListener{
    private static final String TAG = "GestionTickets";
    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private TabLayout tabLayout;
    private List<Ticket> ticketList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private OkHttpClient client;
    private static final String API_URL = "https://66fd14c5c3a184a84d18ff38.mockapi.io/generador";
    private String currentUserRole;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_tickets);
        // Inicializar debugging
        Log.d(TAG, "onCreate: Iniciando GestionTickets");
        // Obtener rol de usuario
        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        currentUserRole = prefs.getString("role", "");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "Usuario actual - Role: " + currentUserRole + ", ID: " + currentUserId);

        // Inicializar OkHttpClient
        client = new OkHttpClient();

        // Inicializar vistas
        initializeViews();
        setupRecyclerViewWithRole();
        setupTabLayout();
        setupSwipeRefresh();

        // Cargar tickets iniciales
        loadTickets("all");
    }
    private void initializeViews() {
        rvTickets = findViewById(R.id.rvTickets);
        tabLayout = findViewById(R.id.tabLayout);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        Log.d(TAG, "Vista inicializada correctamente");
    }
    private void setupRecyclerViewWithRole() {
        ticketList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(ticketList, currentUserRole, this);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(ticketAdapter);
        Log.d(TAG, "RecyclerView configurado con role: " + currentUserRole);
    }
    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadTickets(getStatusForTab(tab.getPosition()));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadTickets(getStatusForTab(tabLayout.getSelectedTabPosition()));
        });
    }
    private String getStatusForTab(int position) {
        switch (position) {
            case 0: return Ticket.STATUS_PENDING;
            case 1: return Ticket.STATUS_IN_PROGRESS;
            case 2: return Ticket.STATUS_COMPLETED;
            default: return Ticket.STATUS_PENDING;
        }
    }
    private void loadTickets(String status) {
        showLoading(true);

        // First load tickets from MockAPI
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<Ticket> mockApiTickets = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonTicket = jsonArray.getJSONObject(i);
                            Ticket ticket = parseTicket(jsonTicket);
                            mockApiTickets.add(ticket);
                        }

                        // Then sync with Firebase for additional data
                        syncTicketsWithFirebase(mockApiTickets, status);

                    } catch (JSONException e) {
                        handleError("Error processing data: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                handleError("Connection error: " + e.getMessage());
            }
        });
    }
    private void syncTicketsWithFirebase(List<Ticket> mockApiTickets, String status) {
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference("tickets");

        ticketsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (Ticket ticket : mockApiTickets) {
                    DataSnapshot ticketSnapshot = dataSnapshot.child(ticket.getTicketNumber());
                    if (ticketSnapshot.exists()) {
                        // Update ticket with Firebase data
                        String assignedWorkerId = ticketSnapshot.child("assignedWorkerId").getValue(String.class);
                        String assignedWorkerName = ticketSnapshot.child("assignedWorkerName").getValue(String.class);
                        String currentStatus = ticketSnapshot.child("status").getValue(String.class);

                        ticket.setAssignedWorkerId(assignedWorkerId);
                        ticket.setAssignedWorkerName(assignedWorkerName);
                        if (currentStatus != null) {
                            ticket.setStatus(currentStatus);
                        }
                    }
                }

                // Filter tickets based on status
                List<Ticket> filteredTickets = mockApiTickets.stream()
                        .filter(ticket -> status.equals("all") || ticket.getStatus().equals(status))
                        .collect(Collectors.toList());

                runOnUiThread(() -> {
                    ticketList.clear();
                    ticketList.addAll(filteredTickets);
                    ticketAdapter.notifyDataSetChanged();
                    showLoading(false);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleError("Firebase sync error: " + error.getMessage());
            }
        });
    }
    private void handleError(String message) {
        runOnUiThread(() -> {
            showLoading(false);
            Toast.makeText(GestionTickets.this, message, Toast.LENGTH_LONG).show();
        });
    }
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }
    @Override
    public void onTicketAction(Ticket ticket, String action) {
        switch (action) {
            case "UPDATE_STATUS":
                showStatusUpdateDialog(ticket);
                break;
            case "UPDATE_PRIORITY":
                showPriorityUpdateDialog(ticket);
                break;
            case "ASSIGN_WORKER":
                showWorkerAssignmentDialog(ticket);
                break;
        }
    }

    private void showStatusUpdateDialog(final Ticket ticket) {
        if (ticket == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Actualizar Estado")
                .setItems(new String[]{"Pendiente", "En Proceso", "Terminado"}, (dialog, which) -> {
                    String newStatus;
                    switch (which) {
                        case 0: newStatus = Ticket.STATUS_PENDING; break;
                        case 1: newStatus = Ticket.STATUS_IN_PROGRESS; break;
                        case 2: newStatus = Ticket.STATUS_COMPLETED; break;
                        default: return;
                    }
                    ticket.updateStatus(newStatus, currentUserId, FirebaseDatabase.getInstance().getReference());
                    loadTickets(getStatusForTab(tabLayout.getSelectedTabPosition()));
                })
                .show();
    }
    private void showPriorityUpdateDialog(final Ticket ticket) {
        if (ticket == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Actualizar Prioridad")
                .setItems(new String[]{"Baja", "Normal", "Alta"}, (dialog, which) -> {
                    String newPriority;
                    switch (which) {
                        case 0: newPriority = Ticket.PRIORITY_LOW; break;
                        case 1: newPriority = Ticket.PRIORITY_NORMAL; break;
                        case 2: newPriority = Ticket.PRIORITY_HIGH; break;
                        default: return;
                    }
                    ticket.updatePriority(newPriority, FirebaseDatabase.getInstance().getReference());
                    loadTickets(getStatusForTab(tabLayout.getSelectedTabPosition()));
                })
                .show();
    }
    private void showWorkerAssignmentDialog(final Ticket ticket) {
        DatabaseReference workersRef = FirebaseDatabase.getInstance().getReference("workers");

        workersRef.get().addOnSuccessListener(dataSnapshot -> {
            List<String> workerNames = new ArrayList<>();
            List<String> workerIds = new ArrayList<>();

            for (DataSnapshot worker : dataSnapshot.getChildren()) {
                String workerName = worker.child("name").getValue(String.class);
                if (workerName != null) {
                    workerNames.add(workerName);
                    workerIds.add(worker.getKey());
                }
            }

            if (workerNames.isEmpty()) {
                Toast.makeText(this, "No hay trabajadores disponibles", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Asignar Trabajador")
                    .setItems(workerNames.toArray(new String[0]), (dialog, which) -> {
                        String selectedWorkerId = workerIds.get(which);
                        String selectedWorkerName = workerNames.get(which);

                        // Update in Firebase
                        DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                                .getReference("tickets")
                                .child(ticket.getTicketNumber());

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("assignedWorkerId", selectedWorkerId);
                        updates.put("assignedWorkerName", selectedWorkerName);

                        ticketRef.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    ticket.setAssignedWorkerId(selectedWorkerId);
                                    ticket.setAssignedWorkerName(selectedWorkerName);
                                    ticketAdapter.notifyDataSetChanged();
                                    Toast.makeText(this, "Trabajador asignado exitosamente",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Error al asignar trabajador", Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }
    private Ticket parseTicket(JSONObject jsonTicket) throws JSONException {
        String ticketNumber = jsonTicket.getString("ticketNumber");
        String problemSpinner = jsonTicket.getString("problemSpinner");
        String area_problema = jsonTicket.getString("area_problema");
        String detalle = jsonTicket.getString("detalle");
        String imagen = jsonTicket.optString("imagen", "");
        String id = jsonTicket.getString("id");
        String createdBy = jsonTicket.optString("createdBy", "Usuario");
        String creationDate = jsonTicket.optString("creationDate", "Fecha no disponible");
        String userId = jsonTicket.optString("userId", "");

        Ticket ticket = new Ticket(ticketNumber, problemSpinner, area_problema,
                detalle, imagen, id, createdBy, creationDate, userId);

        // Establecer campos adicionales si están disponibles
        ticket.setStatus(jsonTicket.optString("status", Ticket.STATUS_PENDING));
        ticket.setPriority(jsonTicket.optString("priority", "Normal"));
        ticket.setAssignedWorkerId(jsonTicket.optString("assignedWorkerId", ""));
        ticket.setAssignedWorkerName(jsonTicket.optString("assignedWorkerName", ""));
        ticket.setLastUpdated(jsonTicket.optString("lastUpdated", creationDate));
        ticket.setComments(jsonTicket.optString("comments", ""));

        return ticket;
    }
}