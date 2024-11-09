package com.example.gemerador.User_Admin;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class AdminUserManager extends AppCompatActivity {
    private EditText emailEditText;
    private Button promoteButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String TAG = "AdminUserManager";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_manager);

        initializeViews();
        setupFirebase();
        checkAdminPermissions();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        promoteButton = findViewById(R.id.promoteButton);
        Button backButton = findViewById(R.id.backButton);

        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void checkAdminPermissions() {
        String currentUserId = mAuth.getCurrentUser().getUid();
        mDatabase.child("Usuarios").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = snapshot.child("role").getValue(String.class);
                if ("Administrador".equals(role) || "Admin".equals(role)) {
                    setupButtons();
                } else {
                    Toast.makeText(AdminUserManager.this,
                            "No tienes permisos de administrador",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminUserManager.this,
                        "Error al verificar permisos: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupButtons() {
        if (promoteButton != null) {
            promoteButton.setOnClickListener(v -> validateAndPromoteUser());
            promoteButton.setEnabled(true);
        }
    }

    private void validateAndPromoteUser() {
        String email = emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("El email es requerido");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Por favor ingrese un email válido");
            return;
        }

        showConfirmationDialog(email);
    }

    private void showConfirmationDialog(final String email) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar promoción")
                .setMessage("¿Está seguro que desea promover a administrador al usuario con email: " + email + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> promoteUserToAdmin(email))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void promoteUserToAdmin(final String email) {
        promoteButton.setEnabled(false);

        mDatabase.child("Usuarios")
                .orderByChild("email")
                .equalTo(email)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            showError("Usuario no encontrado");
                            return;
                        }

                        boolean userPromoted = false;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            Boolean isAdmin = userSnapshot.child("esAdmin").getValue(Boolean.class);
                            if (isAdmin != null && isAdmin) {
                                showError("El usuario ya es administrador");
                                return;
                            }

                            String estado = userSnapshot.child("estado").getValue(String.class);
                            if (!"activo".equals(estado)) {
                                showError("El usuario debe estar activo para ser promovido");
                                return;
                            }

                            String userId = userSnapshot.getKey();
                            updateUserToAdmin(userId, email);
                            userPromoted = true;
                            break;
                        }

                        if (!userPromoted) {
                            showError("No se pudo completar la promoción");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        showError("Error en la base de datos: " + databaseError.getMessage());
                    }
                });
    }

    private void updateUserToAdmin(String userId, String email) {
        DatabaseReference userRef = mDatabase.child("Usuarios").child(userId);

        // Actualizamos múltiples campos de manera atómica
        userRef.updateChildren(Map.of(
                "esAdmin", true,
                "role", "Administrador"
        )).addOnSuccessListener(aVoid -> {
            showSuccess("Usuario promovido a administrador exitosamente");
            clearFields();
        }).addOnFailureListener(e ->
                showError("Error al promover usuario: " + e.getMessage())
        ).addOnCompleteListener(task ->
                promoteButton.setEnabled(true)
        );
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            promoteButton.setEnabled(true);
        });
    }

    private void showSuccess(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        );
    }

    private void clearFields() {
        emailEditText.setText("");
    }
}