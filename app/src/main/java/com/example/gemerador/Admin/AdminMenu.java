package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Gestion.GestionTickets;
import com.example.gemerador.MainActivity;
import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.example.gemerador.User_Admin.AdminUserManager;
import com.google.firebase.auth.FirebaseAuth;


import java.util.List;

public class AdminMenu extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private AdminSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_menu);
        sessionManager = new AdminSessionManager();
        Log.d("AdminMenu", "AdminMenu activity started");

        mAuth = FirebaseAuth.getInstance();
        Button btnAgregarUsuario = findViewById(R.id.btnCrearUsuario);
        btnAgregarUsuario.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMenu.this, Crear_User.class);
            startActivity(intent);
        });
        findViewById(R.id.btnManageUsers).setOnClickListener(v -> startActivity(new Intent(this, AdminUserManager.class)));
        findViewById(R.id.btnCrearUsuario).setOnClickListener(v -> startActivity(new Intent(this, Crear_User.class)));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> cerrarSesion());

        // Añadir botón para gestionar solicitudes de registro
        findViewById(R.id.btnManageUsers).setOnClickListener(v -> startActivity(new Intent(this, GestionSolicitudesActivity.class)));
        // Boton para modo admin
        Button userAdminButton = findViewById(R.id.user_admin);
        if (userAdminButton != null) {
            userAdminButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(AdminMenu.this, AdminUserManager.class);
                    startActivity(intent);
                }
            });
        }
        //Boton para gestionar los tickets
        Button gestion_tickets =findViewById(R.id.gestion_tickets);
        if (gestion_tickets !=null){
            gestion_tickets.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   Intent intent =new Intent(AdminMenu.this, GestionTickets.class);
                    startActivity(intent);
                }
            });
        }
    }
    private void cerrarSesion() {
        sessionManager.signOut(() -> {
            // Redireccionar al MainActivity
            Intent intent = new Intent(AdminMenu.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionManager.startListening(new AdminSessionManager.OnSolicitudesLoadListener() {
            @Override
            public void onSolicitudesLoaded(List<Solicitud> solicitudes) {
                // Actualizar tu RecyclerView aquí
            }

            @Override
            public void onError(String error) {
                Toast.makeText(AdminMenu.this,
                        "Error al cargar solicitudes: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        sessionManager.stopListening();
    }
}