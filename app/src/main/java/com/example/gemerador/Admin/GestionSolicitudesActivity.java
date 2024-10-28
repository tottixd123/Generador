package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.SolicitudAdapter;
import com.example.gemerador.Inicio_User.Usuario;
import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GestionSolicitudesActivity extends AppCompatActivity implements SolicitudAdapter.OnSolicitudListener {
    private static final String TAG = "GestionSolicitudes";
    public static final String EXTRA_SOLICITUD = "solicitudes_registro";
    private RecyclerView recyclerView;
    private SolicitudAdapter adapter;
    private List<Solicitud> solicitudes = new ArrayList<>();
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gestion_solicitudes);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.recyclerViewSolicitudes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SolicitudAdapter(solicitudes, this);
        recyclerView.setAdapter(adapter);

        // Verificar permisos antes de cargar solicitudes
        verificarPermisosYCargarSolicitudes();
    }

    private void verificarPermisosYCargarSolicitudes() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            // Aquí podrías redirigir al login si lo deseas
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(currentUser.getUid());

        userRef.child("role").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String role = task.getResult().getValue(String.class);
                if ("Administrador".equals(role)) {
                    // Usuario es administrador, inicializar la base de datos y cargar solicitudes
                    mDatabase = FirebaseDatabase.getInstance().getReference("solicitudes_registro");
                    cargarSolicitudes();
                } else {
                    Toast.makeText(this, "No tienes permisos de administrador", Toast.LENGTH_LONG).show();
                    finish(); // Cerrar la actividad
                }
            } else {
                Toast.makeText(this, "Error al verificar permisos: " + task.getException().getMessage(),
                        Toast.LENGTH_LONG).show();
                finish(); // Cerrar la actividad
            }
        });
    }

    private void cargarSolicitudes() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                solicitudes.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Solicitud solicitud = snapshot.getValue(Solicitud.class);
                        if (solicitud != null) {
                            // Asegurarse de que el ID esté establecido
                            solicitud.setId(snapshot.getKey());
                            // Solo agregar solicitudes pendientes
                            if ("pendiente".equals(solicitud.getEstado())) {
                                solicitudes.add(solicitud);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al convertir solicitud: " + e.getMessage());
                    }
                }
                adapter.notifyDataSetChanged();

                // Mostrar mensaje si no hay solicitudes
                if (solicitudes.isEmpty()) {
                    // Aquí podrías mostrar una vista de "no hay solicitudes" en lugar de un Toast
                    Toast.makeText(GestionSolicitudesActivity.this,
                            "No hay solicitudes pendientes",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error al cargar solicitudes: " + databaseError.getMessage());
                Toast.makeText(GestionSolicitudesActivity.this,
                        "Error al cargar las solicitudes",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    public void onAprobarClick(Solicitud solicitud) {
        try {
            Intent intent = new Intent(GestionSolicitudesActivity.this, AutoCrearUsuario.class);
            intent.putExtra(EXTRA_SOLICITUD, solicitud);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("GestionSolicitudes", "Error al lanzar AutoCrearUsuarioActivity: " + e.getMessage());
            Toast.makeText(this, "Error al procesar la solicitud", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRechazarClick(Solicitud solicitud) {
        mDatabase.child(solicitud.getId())
                .child("estado")
                .setValue("rechazado")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(GestionSolicitudesActivity.this,
                            "Solicitud rechazada",
                            Toast.LENGTH_SHORT).show();
                    cargarSolicitudes();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(GestionSolicitudesActivity.this,
                                "Error al rechazar la solicitud",
                                Toast.LENGTH_SHORT).show());
    }
}