package com.example.gemerador.Trabajador;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class TrabajadorTicketActivity extends AppCompatActivity implements TicketAdapter.TicketActionListener {
    private RecyclerView recyclerView;
    private TicketAdapter ticketAdapter;
    private TrabajadorService trabajadorService;
    private String currentUserId;
    private Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajador_ticket);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        trabajadorService = new TrabajadorService(currentUserId);

        initializeViews();
        setupRecyclerView();
        setupFilterSpinner();
        loadTickets();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewTickets);
        filterSpinner = findViewById(R.id.spinnerFilter);
    }

    private void setupRecyclerView() {
        ticketAdapter = new TicketAdapter(new ArrayList<>(), "Trabajador", this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(ticketAdapter);
    }

    private void setupFilterSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ticket_status_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String filter = parent.getItemAtPosition(position).toString();
                if (filter.equals("Todos")) {
                    ticketAdapter.searchTickets("");
                } else {
                    ticketAdapter.searchTickets(filter);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadTickets() {
        trabajadorService.loadAssignedTickets(new TrabajadorService.OnTicketsLoadedListener() {
            @Override
            public void onTicketsLoaded(List<Ticket> tickets) {
                ticketAdapter = new TicketAdapter(tickets, "Trabajador", TrabajadorTicketActivity.this);
                recyclerView.setAdapter(ticketAdapter);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TrabajadorTicketActivity.this,
                        "Error al cargar tickets: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTicketAction(Ticket ticket, String action) {
        switch (action) {
            case "UPDATE_STATUS":
                showUpdateStatusDialog(ticket);
                break;
            case "ADD_COMMENT":
                showAddCommentDialog(ticket);
                break;
        }
    }

    private void showUpdateStatusDialog(final Ticket ticket) {
        // Implementa un AlertDialog para seleccionar el nuevo estado
        trabajadorService.updateTicketStatus(ticket.getId(), "NuevoEstado", new TrabajadorService.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TrabajadorTicketActivity.this,
                        "Estado actualizado correctamente", Toast.LENGTH_SHORT).show();
                loadTickets(); // Recargar tickets para mostrar cambios
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TrabajadorTicketActivity.this,
                        "Error al actualizar estado: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCommentDialog(final Ticket ticket) {
        // Implementa un AlertDialog para agregar comentario
        trabajadorService.addTicketComment(ticket.getId(), "Nuevo comentario", new TrabajadorService.OnOperationCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(TrabajadorTicketActivity.this,
                        "Comentario agregado", Toast.LENGTH_SHORT).show();
                loadTickets(); // Recargar tickets para mostrar cambios
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TrabajadorTicketActivity.this,
                        "Error al agregar comentario: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trabajadorService != null) {
            trabajadorService.cleanup();
        }
    }
}