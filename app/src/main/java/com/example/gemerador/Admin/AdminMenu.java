package com.example.gemerador.Admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.Gestion.GestionTicketsActivity;
import com.example.gemerador.Lista.UserListActivity;
import com.example.gemerador.MainActivity;
import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.example.gemerador.Trabajador.TrabajadorManagement;
import com.example.gemerador.User_Admin.AdminUserManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class AdminMenu extends AppCompatActivity {
    private static final String TAG = "AdminMenu";
    private FirebaseAuth mAuth;
    private AdminSessionManager sessionManager;
    private boolean isSessionManagerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_menu);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Verify admin access and initialize session
        initializeAdminSession();
    }

    private void initializeAdminSession() {
        AdminAuthManager.getInstance().verifyAdminAccess(isAdmin -> {
            if (!isAdmin) {
                Toast.makeText(this, "No tienes permisos de administrador", Toast.LENGTH_SHORT).show();
                redirectToMain();
                return;
            }

            try {
                // Initialize session manager
                if (!isSessionManagerInitialized) {
                    sessionManager = new AdminSessionManager();
                    isSessionManagerInitialized = true;
                    Log.d(TAG, "SessionManager initialized successfully");

                    // Start listening for solicitudes
                    startListeningForSolicitudes();

                    // Initialize UI only after session is ready
                    initializeMenu();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing SessionManager: " + e.getMessage());
                Toast.makeText(this, "Error al iniciar sesión: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                redirectToMain();
            }
        });
    }

    private void startListeningForSolicitudes() {
        if (sessionManager != null) {
            sessionManager.startListening(new AdminSessionManager.OnSolicitudesLoadListener() {
                @Override
                public void onSolicitudesLoaded(List<Solicitud> solicitudes) {
                    Log.d(TAG, "Solicitudes loaded successfully: " + solicitudes.size());
                    // Handle loaded solicitudes here if needed
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading solicitudes: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(AdminMenu.this,
                                "Error al cargar solicitudes: " + error,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void initializeMenu() {
        if (!isSessionManagerInitialized) {
            Log.e(TAG, "Attempting to initialize menu before SessionManager");
            return;
        }

        Log.d(TAG, "Initializing admin menu");

        // Initialize buttons
        Button btnAgregarUsuario = findViewById(R.id.btnCrearUsuario);
        Button userAdminButton = findViewById(R.id.user_admin);
        Button gestionTickets = findViewById(R.id.gestion_tickets);
        Button btnWorkerManagement = findViewById(R.id.btnWorkerManagement);
        Button btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Set click listeners
        btnAgregarUsuario.setOnClickListener(v -> startActivity(new Intent(this, Crear_User.class)));

        findViewById(R.id.btnManageUsers).setOnClickListener(v ->
                startActivity(new Intent(this, GestionSolicitudesActivity.class)));

        btnCerrarSesion.setOnClickListener(v -> cerrarSesion());

        userAdminButton.setOnClickListener(v ->
                startActivity(new Intent(this, AdminUserManager.class)));

        gestionTickets.setOnClickListener(v ->
                startActivity(new Intent(this, GestionTicketsActivity.class)));

        btnWorkerManagement.setOnClickListener(v ->
                startActivity(new Intent(this, TrabajadorManagement.class)));
        Button btnListaUser = findViewById(R.id.btnListaUser);
        btnListaUser.setOnClickListener(v ->
                startActivity(new Intent(this, UserListActivity.class)));
    }
    private void cerrarSesion() {
        // Limpiar SharedPreferences
        getSharedPreferences("UserData", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Cerrar sesión del administrador
        if (sessionManager != null) {
            sessionManager.signOut(() -> {
                // Limpiar sesión del administrador
                sessionManager.stopListening();
                sessionManager = null;
                isSessionManagerInitialized = false;

                // Cerrar sesión en Firebase
                if (mAuth != null) {
                    mAuth.signOut();
                }

                // Redireccionar al MainActivity
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        } else {
            // Si no hay sessionManager, hacer cierre de sesión simple
            if (mAuth != null) {
                mAuth.signOut();
            }

            // Redireccionar al MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    private void redirectToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Verify session state
        if (!isSessionManagerInitialized) {
            initializeAdminSession();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sessionManager != null) {
            sessionManager.stopListening();
        }
    }
}