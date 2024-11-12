package com.example.gemerador.Trabajador;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import static com.example.gemerador.Trabajador.TrabajadorServiceCallbacks.*;


public class TrabajadorMenu extends AppCompatActivity {
    private static final String TAG = "TrabajadorMenu";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerViewTickets;
    private TrabajadorTicketsAdapter ticketsAdapter;
    private TrabajadorService trabajadorService;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajador_menu);

        initializeFirebase();
        checkCurrentUser();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        userId = currentUser.getUid();
        verificarRolTrabajador();
    }

    private void verificarRolTrabajador() {
        mDatabase.child("Usuarios").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.child("role").getValue(String.class);
                if ("Trabajador".equals(role)) {
                    saveUserRole(role);
                    setupComponents();
                } else {
                    Toast.makeText(TrabajadorMenu.this,
                            "No tienes permisos de trabajador", Toast.LENGTH_SHORT).show();
                    redirectToLogin();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TrabajadorMenu.this,
                        "Error al verificar permisos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                redirectToLogin();
            }
        });
    }

    private void setupComponents() {
        initializeViews();
        setupTrabajadorService();
        loadAssignedTickets();
    }

    private void saveUserRole(String role) {
        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
        prefs.edit().putString("role", role).apply();
    }

    private void initializeViews() {
        recyclerViewTickets = findViewById(R.id.recyclerViewTickets);
        recyclerViewTickets.setLayoutManager(new LinearLayoutManager(this));

        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());
    }

    private void setupTrabajadorService() {
        trabajadorService = new TrabajadorService(userId);
    }
    private void loadAssignedTickets() {
        if (trabajadorService != null) {
            trabajadorService.loadAssignedTickets(new TrabajadorServiceCallbacks.OnTicketsLoadedListener() {
                @Override
                public void onTicketsLoaded(List<Ticket> tickets) {
                    runOnUiThread(() -> setupAdapter(tickets));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(TrabajadorMenu.this,
                                    "Error al cargar tickets: " + error,
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } else {
            Toast.makeText(this, "Error: Servicio no inicializado", Toast.LENGTH_SHORT).show();
        }
    }
    private void setupAdapter(List<Ticket> tickets) {
        runOnUiThread(() -> {
            ticketsAdapter = new TrabajadorTicketsAdapter(tickets, ticket -> {
                Intent intent = new Intent(this, TrabajadorTicketDetailActivity.class);
                intent.putExtra("ticketId", ticket.getId());
                startActivity(intent);
            });
            recyclerViewTickets.setAdapter(ticketsAdapter);
        });
    }
    private void cerrarSesion() {
        // Primero desuscribirse de todos los listeners de Firebase
        if (trabajadorService != null) {
            trabajadorService.cleanup();
            trabajadorService = null;
        }

        // Limpiar SharedPreferences
        getSharedPreferences("UserData", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Cerrar sesión en Firebase y esperar a que se complete
        mAuth.signOut();

        // Limpiar la referencia a la base de datos
        mDatabase = null;

        // Asegurarse de que la redirección ocurra después de la limpieza
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 100);
    }
    private void redirectToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}