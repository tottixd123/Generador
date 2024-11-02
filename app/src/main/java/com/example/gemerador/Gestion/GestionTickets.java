package com.example.gemerador.Gestion;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.android.material.tabs.TabLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GestionTickets extends AppCompatActivity {
    private RecyclerView rvTickets;
    private TicketAdapter ticketAdapter;
    private TabLayout tabLayout;
    private List<Ticket> ticketList;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private OkHttpClient client;
    private static final String API_URL = "https://66fd14c5c3a184a84d18ff38.mockapi.io/generador";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_tickets);

        // Inicializar OkHttpClient
        client = new OkHttpClient();

        // Inicializar vistas
        initializeViews();
        setupRecyclerView();
        setupTabLayout();
        setupSwipeRefresh();

        // Cargar tickets iniciales
        loadTickets(Ticket.STATUS_PENDING);
    }

    private void initializeViews() {
        rvTickets = findViewById(R.id.rvTickets);
        tabLayout = findViewById(R.id.tabLayout);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
    }

    private void setupRecyclerView() {
        ticketList = new ArrayList<>();
        ticketAdapter = new TicketAdapter(ticketList);
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(ticketAdapter);
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
            case 0:
                return Ticket.STATUS_PENDING;
            case 1:
                return Ticket.STATUS_IN_PROGRESS;
            case 2:
                return Ticket.STATUS_COMPLETED;
            default:
                return Ticket.STATUS_PENDING;
        }
    }

    private void loadTickets(String status) {
        showLoading(true);
        Request request = new Request.Builder()
                .url(API_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(GestionTickets.this,
                            "Error al cargar tickets: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        List<Ticket> newTickets = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonTicket = jsonArray.getJSONObject(i);
                            Ticket ticket = parseTicket(jsonTicket);
                            if (ticket.getStatus().equals(status)) {
                                newTickets.add(ticket);
                            }
                        }

                        runOnUiThread(() -> {
                            ticketList.clear();
                            ticketList.addAll(newTickets);
                            ticketAdapter.notifyDataSetChanged();
                            showLoading(false);
                        });

                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            showLoading(false);
                            Toast.makeText(GestionTickets.this,
                                    "Error al procesar datos: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(GestionTickets.this,
                                "Error en la respuesta del servidor",
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
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

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(false);
        }
    }
}