package com.example.gemerador;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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

    private static final int LOGIN_REQUEST_CODE = 1;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate iniciado");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "setContentView completado");

        try {
            // Inicializar Firebase Auth
            mAuth = FirebaseAuth.getInstance();
            Log.d("MainActivity", "Firebase Auth inicializado");
            // Verificar conexión a Firebase
            checkFirebaseConnection();

            // Configurar el botón de inicio de registro
            Button registro = findViewById(R.id.inicio_registro);
            registro.setOnClickListener(v -> {
                Intent intent = new Intent(this, Nuevo_Registro.class);
                startActivity(intent);
            });

            // Configurar el botón de inicio de sesión
            Button iniciar = findViewById(R.id.iniciar_sesion);
            iniciar.setOnClickListener(v -> iniciarSesion());

            // Verificar si el usuario ya está autenticado
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                // Usuario ya autenticado, verificar si es admin
                verificarAdmin(currentUser.getUid());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error en onCreate", e);
            Toast.makeText(this, "Error al iniciar la aplicación", Toast.LENGTH_LONG).show();
        }
    }
    private void checkFirebaseConnection() {
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    Log.d("FirebaseConnection", "Conectado a Firebase");
                    Toast.makeText(MainActivity.this, "Conectado a la base de datos", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("FirebaseConnection", "Desconectado de Firebase");
                    new Handler().postDelayed(() -> {
                        if (connected == null || !connected) {
                            Toast.makeText(MainActivity.this, "No hay conexión a la base de datos", Toast.LENGTH_LONG).show();
                        }
                    }, 5000);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseConnection", "Error en la conexión: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error al conectar con la base de datos: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarSesion() {
        Intent intent = new Intent(this, Login.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }

    private void verificarAdmin(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean esAdmin = dataSnapshot.child("esAdmin").getValue(Boolean.class);
                    if (esAdmin != null && esAdmin) {
                        // Es admin, ir a AdminMenu
                        irAAdminMenu();
                    } else {
                        // No es admin, ir a la actividad normal de usuario
                        irAInicioUser();
                    }
                } else {
                    // El usuario no existe en la base de datos
                    Toast.makeText(MainActivity.this, "Error: Usuario no encontrado", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error al verificar rol de usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void irAAdminMenu() {
        Intent intent = new Intent(MainActivity.this, AdminMenu.class);
        startActivity(intent);
        finish();
    }

    private void irAInicioUser() {
        Intent intent = new Intent(MainActivity.this, Inicio_User.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // El inicio de sesión fue exitoso
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    verificarAdmin(user.getUid());
                }
            } else if (resultCode == RESULT_CANCELED) {
                // El usuario canceló el inicio de sesión
                Toast.makeText(this, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}