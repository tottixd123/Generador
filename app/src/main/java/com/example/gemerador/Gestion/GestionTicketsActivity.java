package com.example.gemerador.Gestion;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gemerador.R;
import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Locale;

public class GestionTicketsActivity extends AppCompatActivity implements TicketAdapter.TicketActionListener {
    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private List<Ticket> ticketList;
    private List<Ticket> originalTicketList;
    private DatabaseReference dbRef;
    private static final String MOCKAPI_URL = "https://66fd14c5c3a184a84d18ff38.mockapi.io/generador";
    private List<String> workerNames = new ArrayList<>();
    private List<String> workerIds = new ArrayList<>();
    private Spinner spinnerWorkers;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private SwipeRefreshLayout swipeRefreshLayout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_tickets);
        // Inicializar y configurar el RecyclerView
        recyclerView = findViewById(R.id.rvTickets);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketAdapter(new ArrayList<>(), "Administrador", this);
        recyclerView.setAdapter(adapter);

        initializeViews();
        setupSpinners();
        setupRecyclerView();
        setupSwipeRefresh();
        loadWorkers();
        // Inicializar listas
        ticketList = new ArrayList<>();
        originalTicketList = new ArrayList<>();

        // Configurar el adapter inmediatamente
        adapter = new TicketAdapter(ticketList, "Administrador", this);
        recyclerView.setAdapter(adapter);

        // Cargar tickets iniciales
        loadTickets();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.rvTickets);
        spinnerWorkers = findViewById(R.id.spinnerWorkers);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        spinnerPriority = findViewById(R.id.spinnerPriority);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        dbRef = FirebaseDatabase.getInstance().getReference();
        ticketList = new ArrayList<>();
        originalTicketList = new ArrayList<>();
    }

    private void setupSpinners() {
        // Configurar Spinner de Estado
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("Todos los estados", Ticket.STATUS_PENDING,
                        Ticket.STATUS_IN_PROGRESS, Ticket.STATUS_COMPLETED));
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Configurar Spinner de Prioridad
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("Todas las prioridades", Ticket.PRIORITY_LOW,
                        Ticket.PRIORITY_NORMAL, Ticket.PRIORITY_HIGH));
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);

        setupSpinnerListeners();
    }

    private void loadWorkers() {
        // Cargar trabajadores desde Realtime Database
        dbRef.child("trabajadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                workerNames.clear();
                workerIds.clear();

                // Agregar opción "Todos los trabajadores" al inicio
                workerNames.add("Todos los trabajadores");
                workerIds.add("");

                for (DataSnapshot workerSnapshot : dataSnapshot.getChildren()) {
                    String workerId = workerSnapshot.getKey();
                    String workerName = workerSnapshot.child("nombre").getValue(String.class);

                    if (workerId != null && workerName != null) {
                        workerNames.add(workerName);
                        workerIds.add(workerId);
                    }
                }

                // Actualizar el Spinner de trabajadores
                ArrayAdapter<String> workerAdapter = new ArrayAdapter<>(
                        GestionTicketsActivity.this,
                        android.R.layout.simple_spinner_item,
                        workerNames
                );
                workerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerWorkers.setAdapter(workerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(GestionTicketsActivity.this,
                        "Error al cargar trabajadores: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinnerListeners() {
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        };

        spinnerWorkers.setOnItemSelectedListener(filterListener);
        spinnerStatus.setOnItemSelectedListener(filterListener);
        spinnerPriority.setOnItemSelectedListener(filterListener);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadTickets();
            loadWorkers();
        });
    }

    private void setupRecyclerView() {
        adapter = new TicketAdapter(ticketList, "Administrador", this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    private void applyFilters() {
        String selectedWorker = spinnerWorkers.getSelectedItemPosition() > 0 ?
                workerIds.get(spinnerWorkers.getSelectedItemPosition()) : "";
        String selectedStatus = spinnerStatus.getSelectedItemPosition() > 0 ?
                spinnerStatus.getSelectedItem().toString() : "";
        String selectedPriority = spinnerPriority.getSelectedItemPosition() > 0 ?
                spinnerPriority.getSelectedItem().toString() : "";

        List<Ticket> filteredList = new ArrayList<>();

        for (Ticket ticket : originalTicketList) {
            boolean matchesWorker = selectedWorker.isEmpty() ||
                    (ticket.getAssignedWorkerId() != null &&
                            ticket.getAssignedWorkerId().equals(selectedWorker));
            boolean matchesStatus = selectedStatus.isEmpty() ||
                    ticket.getStatus().equals(selectedStatus);
            boolean matchesPriority = selectedPriority.isEmpty() ||
                    ticket.getPriority().equals(selectedPriority);

            if (matchesWorker && matchesStatus && matchesPriority) {
                filteredList.add(ticket);
            }
        }

        ticketList.clear();
        ticketList.addAll(filteredList);
        adapter.setTickets(ticketList); // Actualizar el adapter con la nueva lista
        adapter.notifyDataSetChanged();
    }
    private void loadTickets() {
        swipeRefreshLayout.setRefreshing(true);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(MOCKAPI_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionTicketsActivity.this,
                            "Error al cargar tickets: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(jsonData);
                        List<Ticket> tickets = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Ticket ticket = new Ticket(
                                    jsonObject.getString("ticketNumber"),
                                    jsonObject.optString("problemSpinner", ""),
                                    jsonObject.optString("area_problema", ""),
                                    jsonObject.optString("detalle", ""),
                                    jsonObject.optString("imagen", ""),
                                    jsonObject.optString("id", ""),
                                    jsonObject.optString("createdBy", ""),
                                    jsonObject.optString("creationDate", getCurrentTimestamp()),
                                    jsonObject.optString("userId", "")
                            );

                            ticket.setStatus(jsonObject.optString("status", Ticket.STATUS_PENDING));
                            ticket.setPriority(jsonObject.optString("priority", Ticket.PRIORITY_NORMAL));
                            ticket.setAssignedWorkerId(jsonObject.optString("assignedWorkerId", ""));
                            ticket.setAssignedWorkerName(jsonObject.optString("assignedWorkerName", ""));
                            ticket.setLastUpdated(jsonObject.optString("lastUpdated", getCurrentTimestamp()));
                            ticket.setComments(jsonObject.optString("comments", ""));

                            tickets.add(ticket);
                        }

                        runOnUiThread(() -> {
                            ticketList.clear();
                            ticketList.addAll(tickets);
                            originalTicketList.clear();
                            originalTicketList.addAll(tickets);
                            adapter.notifyDataSetChanged();
                            swipeRefreshLayout.setRefreshing(false);
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(GestionTicketsActivity.this,
                                    "Error al procesar datos: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            swipeRefreshLayout.setRefreshing(false);
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onTicketAction(Ticket ticket, String action) {
        switch (action) {
            case "ASSIGN_WORKER":
                showAssignWorkerDialog(ticket);
                break;
            case "UPDATE_STATUS":
                showUpdateStatusDialog(ticket);
                break;
            case "UPDATE_PRIORITY":
                showUpdatePriorityDialog(ticket);
                break;
        }
    }

    private void showAssignWorkerDialog(Ticket ticket) {
        if (workerNames.isEmpty()) {
            Toast.makeText(this, "No hay trabajadores disponibles", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Asignar Trabajador")
                .setItems(workerNames.toArray(new String[0]), (dialog, which) -> {
                    String selectedWorkerId = workerIds.get(which);
                    String selectedWorkerName = workerNames.get(which);

                    // Actualizar en Realtime Database
                    ticket.assignWorker(selectedWorkerId, selectedWorkerName, dbRef);

                    try {
                        JSONObject updates = new JSONObject();
                        updates.put("assignedWorkerId", selectedWorkerId);
                        updates.put("assignedWorkerName", selectedWorkerName);
                        updates.put("status", Ticket.STATUS_IN_PROGRESS);
                        updates.put("lastUpdated", getCurrentTimestamp());

                        updateTicketInMockAPI(ticket.getTicketNumber(), updates);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private void showUpdateStatusDialog(Ticket ticket) {
        String[] statusOptions = {
                Ticket.STATUS_PENDING,
                Ticket.STATUS_IN_PROGRESS,
                Ticket.STATUS_COMPLETED
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Estado")
                .setItems(statusOptions, (dialog, which) -> {
                    String newStatus = statusOptions[which];
                    ticket.updateStatus(newStatus, "Administrador", dbRef);

                    try {
                        JSONObject updates = new JSONObject();
                        updates.put("status", newStatus);
                        updates.put("lastUpdated", getCurrentTimestamp());
                        updateTicketInMockAPI(ticket.getTicketNumber(), updates);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private void showUpdatePriorityDialog(Ticket ticket) {
        String[] priorityOptions = {
                Ticket.PRIORITY_LOW,
                Ticket.PRIORITY_NORMAL,
                Ticket.PRIORITY_HIGH
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Prioridad")
                .setItems(priorityOptions, (dialog, which) -> {
                    String newPriority = priorityOptions[which];
                    ticket.updatePriority(newPriority, dbRef);

                    try {
                        JSONObject updates = new JSONObject();
                        updates.put("priority", newPriority);
                        updates.put("lastUpdated", getCurrentTimestamp());
                        updateTicketInMockAPI(ticket.getTicketNumber(), updates);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                })
                .show();
    }

    private void updateTicketInMockAPI(String ticketNumber, JSONObject updates) {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, updates.toString());

        Request request = new Request.Builder()
                .url(MOCKAPI_URL + "/" + ticketNumber)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(GestionTicketsActivity.this,
                        "Error al actualizar ticket: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(GestionTicketsActivity.this,
                                "Ticket actualizado exitosamente",
                                Toast.LENGTH_SHORT).show();
                        loadTickets();
                    });
                }
            }
        });
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}