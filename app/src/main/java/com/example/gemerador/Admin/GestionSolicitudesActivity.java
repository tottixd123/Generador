package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.SolicitudAdapter;
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
    private ProgressBar progressBar;
    private TextView tvNoSolicitudes;

    private SolicitudAdapter adapter;
    private List<Solicitud> solicitudes;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener solicitudesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_gestion_solicitudes);

        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        verificarPermisosYCargarSolicitudes();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewSolicitudes);
        progressBar = findViewById(R.id.progressBar);
        tvNoSolicitudes = findViewById(R.id.tvNoSolicitudes);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("solicitudes_registro");
    }

    private void setupRecyclerView() {
        solicitudes = new ArrayList<>();
        adapter = new SolicitudAdapter(solicitudes, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void verificarPermisosYCargarSolicitudes() {
        showLoading(true);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            showError("Usuario no autenticado");
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(currentUser.getUid());

        userRef.child("role").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                showError("Error al verificar permisos: " + task.getException().getMessage());
                finish();
                return;
            }

            String role = task.getResult().getValue(String.class);
            if (!"Administrador".equals(role)) {
                showError("No tienes permisos de administrador");
                finish();
                return;
            }

            cargarSolicitudes();
        });
    }

    private void cargarSolicitudes() {
        solicitudesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                solicitudes.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Solicitud solicitud = snapshot.getValue(Solicitud.class);
                        if (solicitud != null && solicitud.isValid()) {
                            solicitud.setId(snapshot.getKey());
                            if ("pendiente".equals(solicitud.getEstado())) {
                                solicitudes.add(solicitud);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error al convertir solicitud: " + e.getMessage());
                    }
                }

                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showError("Error al cargar solicitudes: " + databaseError.getMessage());
                showLoading(false);
            }
        };

        mDatabase.addValueEventListener(solicitudesListener);
    }

    private void updateUI() {
        showLoading(false);
        adapter.notifyDataSetChanged();

        if (solicitudes.isEmpty()) {
            tvNoSolicitudes.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoSolicitudes.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onAprobarClick(Solicitud solicitud) {
        if (solicitud == null || solicitud.getId() == null) {
            showError("Error: Datos de solicitud inválidos");
            return;
        }

        try {
            Intent intent = new Intent(this, AutoCrearUsuario.class);
            // Pasamos el ID de la solicitud, que es lo que necesitamos para recuperarla de Firebase
            intent.putExtra(AutoCrearUsuario.EXTRA_SOLICITUD, solicitud.getId());
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al lanzar AutoCrearUsuario: " + e.getMessage());
            showError("Error al procesar la solicitud: " + e.getMessage());
        }
    }

    @Override
    public void onRechazarClick(Solicitud solicitud) {
        if (solicitud == null || solicitud.getId() == null) {
            showError("Error: Datos de solicitud inválidos");
            return;
        }

        showLoading(true);
        mDatabase.child(solicitud.getId())
                .child("estado")
                .setValue("rechazado")
                .addOnSuccessListener(aVoid -> {
                    showMessage("Solicitud rechazada");
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showError("Error al rechazar la solicitud");
                    showLoading(false);
                });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && solicitudesListener != null) {
            mDatabase.removeEventListener(solicitudesListener);
        }
    }
}