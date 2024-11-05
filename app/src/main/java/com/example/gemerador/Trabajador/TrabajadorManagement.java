package com.example.gemerador.Trabajador;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gemerador.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class TrabajadorManagement extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TrabajadorManagementAdapter adapter;
    private DatabaseReference usuariosRef;
    private List<Map<String, String>> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajador_management);

        initializeViews();
        setupRecyclerView();
        loadUsers();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios");
        userList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new TrabajadorManagementAdapter(userList, this::promoteToWorker);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadUsers() {
        usuariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String role = snapshot.child("role").getValue(String.class);
                    if (role != null && !role.equals("Administrador") && !role.equals("Trabajador")) {
                        Map<String, String> user = new HashMap<>();
                        user.put("id", snapshot.getKey());
                        user.put("nombre", snapshot.child("nombre").getValue(String.class));
                        user.put("email", snapshot.child("email").getValue(String.class));
                        user.put("area", snapshot.child("area").getValue(String.class));
                        user.put("cargo", snapshot.child("cargo").getValue(String.class));
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(TrabajadorManagement.this,
                        "Error al cargar usuarios: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promoteToWorker(Map<String, String> user) {
        String userId = user.get("id");
        if (userId == null) return;

        DatabaseReference userRef = usuariosRef.child(userId);

        // Crear datos del trabajador
        Map<String, Object> trabajadorData = new HashMap<>();
        trabajadorData.put("role", "Trabajador");
        trabajadorData.put("isAvailable", true);
        trabajadorData.put("activeTickets", 0);

        // Actualizar usuario y crear entrada de trabajador
        userRef.updateChildren(trabajadorData)
                .addOnSuccessListener(aVoid -> {
                    DatabaseReference trabajadoresRef = FirebaseDatabase.getInstance()
                            .getReference("trabajadores").child(userId);

                    Map<String, Object> trabajadorProfile = new HashMap<>();
                    trabajadorProfile.put("nombre", user.get("nombre"));
                    trabajadorProfile.put("email", user.get("email"));
                    trabajadorProfile.put("area", user.get("area"));
                    trabajadorProfile.put("cargo", user.get("cargo"));
                    trabajadorProfile.put("available", true);
                    trabajadorProfile.put("activeTickets", 0);

                    trabajadoresRef.setValue(trabajadorProfile)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(TrabajadorManagement.this,
                                        "Usuario promovido a trabajador exitosamente",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(TrabajadorManagement.this,
                                        "Error al crear perfil de trabajador: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(TrabajadorManagement.this,
                            "Error al promover usuario: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}