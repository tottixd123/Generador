package com.example.gemerador;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.example.gemerador.Admin.AdminMenu;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.Trabajador.TrabajadorMenu;
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
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        requestNotificationPermission();
    }
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Primero verificar SharedPreferences
            SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);
            String savedRole = prefs.getString("role", null);
            String savedUserId = prefs.getString("userId", null);

            if (savedRole != null && savedUserId != null && savedUserId.equals(currentUser.getUid())) {
                // Si tenemos datos guardados, usar esos
                redirectBasedOnRole(savedRole);
            } else {
                // Si no hay datos guardados, verificar con Firebase
                verificarRolUsuario(currentUser.getUid());
            }
        } else {
            setupButtons();
        }
    }

    private void setupButtons() {
        Button registro = findViewById(R.id.inicio_registro);
        registro.setOnClickListener(v -> startActivity(new Intent(this, Nuevo_Registro.class)));

        Button iniciar = findViewById(R.id.iniciar_sesion);
        iniciar.setOnClickListener(v -> startActivity(new Intent(this, Login.class)));
        overridePendingTransition(0, 0);
    }

    private void verificarRolUsuario(String userId) {
        DatabaseReference userRef = mDatabase.child("Usuarios").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String role = dataSnapshot.child("role").getValue(String.class);
                    String nombre = dataSnapshot.child("nombre").getValue(String.class);
                    Log.d(TAG, "User role: " + role);

                    // Guardar en SharedPreferences
                    SharedPreferences.Editor editor = getSharedPreferences("UserData", MODE_PRIVATE).edit();
                    editor.putString("role", role);
                    editor.putString("nombre", nombre);
                    editor.putString("userId", userId);
                    editor.apply();

                    redirectBasedOnRole(role);
                } else {
                    Log.d(TAG, "No se encontró el usuario, posiblemente nuevo registro");
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

    private void redirectBasedOnRole(String role) {
        switch (role) {
            case "Administrador":
                Log.d(TAG, "Usuario es Administrador, redirigiendo a AdminMenu");
                irAAdminMenu();
                break;
            case "Trabajador":
                Log.d(TAG, "Usuario es Trabajador, redirigiendo a TrabajadorMenu");
                irATrabajadorMenu();
                break;
            case "Usuario":
                Log.d(TAG, "Usuario es normal, redirigiendo a InicioUser");
                irAInicioUser();
                break;
            default:
                Log.d(TAG, "Rol no reconocido: " + role);
                Toast.makeText(this, "Rol de usuario no válido", Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                setupButtons();
                break;
        }
    }

    private void crearUsuarioNormal(String userId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference newUserRef = mDatabase.child("Usuarios").child(userId);

            // Crear objeto usuario básico
            newUserRef.child("email").setValue(user.getEmail());
            newUserRef.child("role").setValue("Usuario");
            newUserRef.child("nombre").setValue(user.getEmail().split("@")[0])
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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, puedes mostrar notificaciones
            } else {
                // Permiso denegado, informa al usuario que no podrá recibir notificaciones
                Toast.makeText(this, "No podrás recibir notificaciones sin este permiso", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void irAAdminMenu() {
        Intent intent = new Intent(this, AdminMenu.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irATrabajadorMenu() {
        Intent intent = new Intent(this, TrabajadorMenu.class);
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