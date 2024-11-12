package com.example.gemerador.Perfil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Perfil_user extends AppCompatActivity {
    private TextView usernameTextView, emailTextView;
    private Switch notificationSwitch;
    private Button logoutButton, iniciobtn;
    private ImageView bellIcon;
    private FirebaseAuth mAuth;
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String USER_DATA_PREFS = "UserData";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_user);
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        loadUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        usernameTextView = findViewById(R.id.usernameValue);
        emailTextView = findViewById(R.id.emailValue);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        logoutButton = findViewById(R.id.logoutButton);
        bellIcon = findViewById(R.id.imageView4);

        // Cargar estado de notificaciones
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notificationsEnabled", true);
        notificationSwitch.setChecked(notificationsEnabled);
    }

    private void setupClickListeners() {
        logoutButton.setOnClickListener(v -> {
            Toast.makeText(Perfil_user.this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
            logout();
        });

        bellIcon.setOnClickListener(v -> {
            notificationSwitch.setChecked(!notificationSwitch.isChecked());
            updateNotificationPreference(notificationSwitch.isChecked());
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationPreference(isChecked);
        });
    }

    private void updateNotificationPreference(boolean enabled) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("notificationsEnabled", enabled);
        editor.apply();

        String message = enabled ? "Notificaciones activadas" : "Notificaciones desactivadas";
        Toast.makeText(Perfil_user.this, message, Toast.LENGTH_SHORT).show();
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            emailTextView.setText(userEmail);

            // Intentar obtener el nombre desde SharedPreferences primero
            SharedPreferences prefs = getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE);
            String savedName = prefs.getString("nombre", null);

            if (savedName != null) {
                usernameTextView.setText(savedName);
            } else {
                // Si no hay nombre guardado, usar displayName o email
                String username = currentUser.getDisplayName();
                if (username != null && !username.isEmpty()) {
                    usernameTextView.setText(username);
                } else {
                    String emailUsername = userEmail.split("@")[0];
                    usernameTextView.setText(emailUsername);
                }
            }
        } else {
            redirectToMain();
        }
    }

    private void logout() {
        // 1. Limpiar SharedPreferences de notificaciones
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // 2. Limpiar SharedPreferences de datos de usuario
        getSharedPreferences(USER_DATA_PREFS, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // 3. Cerrar sesión en Firebase
        mAuth.signOut();

        // 4. Mostrar mensaje
        Toast.makeText(Perfil_user.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // 5. Redireccionar al MainActivity
        redirectToMain();
    }

    private void redirectToMain() {
        Intent intent = new Intent(Perfil_user.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}