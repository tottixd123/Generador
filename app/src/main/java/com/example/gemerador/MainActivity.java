package com.example.gemerador;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Admin.AdminMenu;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.Nuevo_Registro.Nuevo_Registro;
import com.example.gemerador.login.Login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        // Then initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            verificarRolUsuario(currentUser.getUid());
        } else {
            setupButtons();
        }
    }

    private void setupButtons() {
        Button registro = findViewById(R.id.inicio_registro);
        registro.setOnClickListener(v -> startActivity(new Intent(this, Nuevo_Registro.class)));

        Button iniciar = findViewById(R.id.iniciar_sesion);
        iniciar.setOnClickListener(v -> startActivity(new Intent(this, Login.class)));
    }

    private void verificarRolUsuario(String userId) {
        DatabaseReference userRef = mDatabase.child("Usuarios").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String role = dataSnapshot.child("role").getValue(String.class);
                    Log.d(TAG, "User role: " + role);

                    if ("Administrador".equals(role)) {
                        Log.d(TAG, "Usuario es Administrador, redirigiendo a AdminMenu");
                        irAAdminMenu();
                    } else {
                        Log.d(TAG, "Usuario es normal, redirigiendo a InicioUser");
                        irAInicioUser();
                    }
                } else {
                    Log.d(TAG, "No se encontró el usuario, posiblemente nuevo registro");
                    // Si el usuario está autenticado pero no tiene datos en la base,
                    // asumimos que es un usuario normal nuevo
                    crearUsuarioNormal(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error verificando rol: " + error.getMessage());
                Toast.makeText(MainActivity.this,
                        "Error al verificar credenciales",
                        Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                setupButtons();
            }
        });
    }

    private void crearUsuarioNormal(String userId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference newUserRef = mDatabase.child("Usuarios").child(userId);

            // Crear objeto usuario básico
            DatabaseReference userRef = newUserRef;
            userRef.child("email").setValue(user.getEmail());
            userRef.child("role").setValue("Usuario");
            userRef.child("nombre").setValue(user.getEmail().split("@")[0])
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Usuario normal creado exitosamente");
                        irAInicioUser();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creando usuario: " + e.getMessage());
                        Toast.makeText(MainActivity.this,
                                "Error al crear perfil de usuario",
                                Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        setupButtons();
                    });
        }
    }

    private void irAAdminMenu() {
        Intent intent = new Intent(this, AdminMenu.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irAInicioUser() {
        Intent intent = new Intent(this, Inicio_User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}