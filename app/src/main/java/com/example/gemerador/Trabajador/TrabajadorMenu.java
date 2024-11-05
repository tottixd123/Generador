package com.example.gemerador.Trabajador;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TrabajadorMenu extends AppCompatActivity {
    private static final String TAG = "TrabajadorMenu";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerViewTickets;
    private TrabajadorTicketsAdapter ticketsAdapter;
    private TrabajadorSessionManager sessionManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trabajador_menu);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupSessionManager();
        loadAssignedTickets();
    }
    private void initializeViews() {
        recyclerViewTickets = findViewById(R.id.recyclerViewTickets);
        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }
    private void setupSessionManager() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            sessionManager = new TrabajadorSessionManager(currentUser.getUid());
        }else {
            redirectToLogin();
        }
    }
    private void loadAssignedTickets(){
        if (sessionManager != null) {
            sessionManager.getAssignedTickets(new TrabajadorSessionManager.TicketsLoadCallback() {
                @Override
                public void onTicketsLoaded(List<Ticket> tickets) {
                    ticketsAdapter  = new TrabajadorTicketsAdapter(tickets, ticket -> {
                        openTicketDetail(ticket);
                    });
                    recyclerViewTickets.setAdapter(ticketsAdapter);
                }
                @Override
                public void onError(String error){
                    Toast.makeText(TrabajadorMenu.this,
                            "Error al cargar los tickets: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void openTicketDetail(Ticket ticket) {
        Intent intent = new Intent(this, TicketDetail.class);
        intent.putExtra("ticketId", ticket.getId());
        startActivity(intent);
    }

    private void cerrarSesion() {
        if (sessionManager != null) {
            sessionManager.signOut();
        }
        mAuth.signOut();
        redirectToLogin();
    }
    private void redirectToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}