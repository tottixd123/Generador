package com.example.gemerador.Lista;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserListActivity";
    private RecyclerView recyclerView;
    private UserListAdapter adapter;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;
    private List<UserModel> userList;
    int color;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_list);

        // Initialize Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewUsers);
        progressBar = findViewById(R.id.progressBar);
        userList = new ArrayList<>();
        adapter = new UserListAdapter(userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadUsers();
    }
    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Iniciando carga de usuarios");

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                userList.clear();

                Log.d(TAG, "Número de usuarios encontrados: " + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        String uid = snapshot.getKey();
                        String nombre = snapshot.child("nombre").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        String cargo = snapshot.child("cargo").getValue(String.class);
                        String area = snapshot.child("area").getValue(String.class);


                        switch (role) {
                            case "Administrador":
                                color = Color.RED;
                                break;
                            case "Usuario":
                                color = Color.BLUE;
                                break;
                            case "Trabajador":
                                color = Color.GREEN;
                                break;
                        }

                        UserModel user = new UserModel(uid, nombre, email, role, cargo, area,color);
                        userList.add(user);

                        Log.d(TAG, "Usuario cargado: " + nombre);
                    } catch (Exception e) {
                        Log.e(TAG, "Error al procesar usuario: " + snapshot.getKey(), e);
                    }
                }

                adapter.notifyDataSetChanged();

                if (userList.isEmpty()) {
                    Log.d(TAG, "No se encontraron usuarios en la lista");
                    Toast.makeText(UserListActivity.this,
                            "No se encontraron usuarios", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error al cargar usuarios: " + databaseError.getMessage());
                Toast.makeText(UserListActivity.this,
                        "Error al cargar usuarios: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove the listener to avoid memory leaks
        if (mDatabase != null) {
            mDatabase.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
}