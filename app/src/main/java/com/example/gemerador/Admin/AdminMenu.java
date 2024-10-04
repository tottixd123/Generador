package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.R;
import com.example.gemerador.login.Login;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminMenu extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_menu);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("usuarios");

        Button btnCrearUsuario = findViewById(R.id.btnCrearUsuario);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        verificarAdmin();

        btnCrearUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMenu.this, Crear_User.class);
            startActivity(intent);
        });

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

    }
    private void verificarAdmin() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Boolean esAdmin = dataSnapshot.child("esAdmin").getValue(Boolean.class);
                    if (esAdmin == null || !esAdmin) {
                        Toast.makeText(AdminMenu.this, "No tienes permisos de administrador", Toast.LENGTH_SHORT).show();
                        cerrarSesion();
                    } else {
                        // El usuario es un administrador, podemos proceder con la carga de la interfaz de administrador
                        cargarInterfazAdmin();
                    }
                } else {
                    Toast.makeText(AdminMenu.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                    cerrarSesion();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminMenu.this, "Error al verificar permisos", Toast.LENGTH_SHORT).show();
                cerrarSesion();
            }
        });
    }
    private void cargarInterfazAdmin() {
        // Aquí puedes inicializar y mostrar los elementos de la interfaz de administrador
        findViewById(R.id.btnCrearUsuario).setVisibility(View.VISIBLE);
        findViewById(R.id.btnCerrarSesion).setVisibility(View.VISIBLE);
        // Añade aquí cualquier otra inicialización necesaria para la interfaz de administrador
    }

    private void cerrarSesion() {
        mAuth.signOut();
        Intent intent = new Intent(AdminMenu.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}