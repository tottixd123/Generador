package com.example.gemerador.Trabajador;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gemerador.Crear_Ti.TicketDetail;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }
    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordEt = dialogView.findViewById(R.id.currentPasswordEt);
        EditText newPasswordEt = dialogView.findViewById(R.id.newPasswordEt);
        EditText confirmPasswordEt = dialogView.findViewById(R.id.confirmPasswordEt);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Cambiar Contraseña")
                .setView(dialogView)
                .setPositiveButton("Cambiar", null)
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String currentPassword = currentPasswordEt.getText().toString();
                String newPassword = newPasswordEt.getText().toString();
                String confirmPassword = confirmPasswordEt.getText().toString();

                if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }
    private boolean validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Reautenticar al usuario
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Actualizar la contraseña
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(TrabajadorMenu.this,
                                                    "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                                            dialog.dismiss();
                                        } else {
                                            Toast.makeText(TrabajadorMenu.this,
                                                    "Error al actualizar la contraseña: " + updateTask.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(TrabajadorMenu.this,
                                    "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
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