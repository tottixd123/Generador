package com.example.gemerador.Admin;

import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Crear_User extends AppCompatActivity {
    private EditText etNombre, etEmail, etPassword, etArea, etCargo;
    private Button btnCrearCuenta;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private CheckBox cbEsAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_user);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");

        initializeViews();
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());
    }
    private void initializeViews() {
        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etArea = findViewById(R.id.etArea);
        etCargo = findViewById(R.id.etCargo);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        cbEsAdmin = findViewById(R.id.cbEsAdmin);
    }


    private void crearCuenta() {
        final String nombre = etNombre.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        final String area = etArea.getText().toString().trim();
        final String cargo = etCargo.getText().toString().trim();
        final boolean esAdmin = cbEsAdmin.isChecked();

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || area.isEmpty() || cargo.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        guardarInformacionUsuario(userId, nombre, email, area, cargo, esAdmin);
                    } else {
                        Toast.makeText(Crear_User.this, "Error al crear la cuenta: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void guardarInformacionUsuario(String userId, String nombre, String email, String area, String cargo, boolean esAdmin) {
        Map<String, Object> user = new HashMap<>();
        user.put("nombre", nombre);
        user.put("email", email);
        user.put("area", area);
        user.put("cargo", cargo);
        user.put("esAdmin", esAdmin);
        // Añadir el rol según el checkbox
        user.put("role", esAdmin ? "Admin" : "Usuario");
        // Añadir estado y fecha de creación
        user.put("estado", "activo");
        user.put("fechaCreacion", System.currentTimeMillis());

        mDatabase.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Crear_User.this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Crear_User.this, "Error al guardar información del usuario: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void limpiarCampos() {
        etNombre.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etArea.setText("");
        etCargo.setText("");
        cbEsAdmin.setChecked(false);
    }
}