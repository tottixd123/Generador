package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.example.gemerador.User_Admin.AdminUserManager;
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
    private Button RegresoSol;
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
        RegresoSol = findViewById(R.id.btnsolicutd);
        RegresoSol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(GestionSolicitudesActivity.this, AdminMenu.class);
                startActivity(intent);
            }
        });
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
        AdminAuthManager.getInstance().verifyAdminAccess(isAdmin -> {
            if (isAdmin) {
                cargarSolicitudes();
            } else {
                showError("No tienes permisos de administrador");
                finish();
            }
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