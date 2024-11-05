package com.example.gemerador.Perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gemerador.Inicio_User.Inicio_User;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_user);
        mAuth = FirebaseAuth.getInstance();

        // Inicializar vistas
        usernameTextView = findViewById(R.id.usernameValue);
        emailTextView = findViewById(R.id.emailValue);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        logoutButton = findViewById(R.id.logoutButton);
        bellIcon = findViewById(R.id.imageView4);

        // Cargar datos del usuario actual
        loadUserData();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(Perfil_user.this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
                logout();
            }
        });


        bellIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationSwitch.setChecked(!notificationSwitch.isChecked());
                String message = notificationSwitch.isChecked() ? "Notificaciones activadas" : "Notificaciones desactivadas";
                Toast.makeText(Perfil_user.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String message = isChecked ? "Notificaciones activadas" : "Notificaciones desactivadas";
            Toast.makeText(Perfil_user.this, message, Toast.LENGTH_SHORT).show();
            // Aquí deberías guardar la preferencia del usuario
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Obtener el email del usuario
            String userEmail = currentUser.getEmail();
            emailTextView.setText(userEmail);

            // Obtener el nombre de usuario (displayName)
            String username = currentUser.getDisplayName();
            if (username != null && !username.isEmpty()) {
                usernameTextView.setText(username);
            } else {
                // Si no hay displayName, usar la primera parte del email como nombre de usuario
                String emailUsername = userEmail.split("@")[0];
                usernameTextView.setText(emailUsername);
            }
        } else {
            // Si no hay usuario logueado, redirigir al login
            Intent intent = new Intent(Perfil_user.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void logout() {
        mAuth.signOut();
        Toast.makeText(Perfil_user.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Perfil_user.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}