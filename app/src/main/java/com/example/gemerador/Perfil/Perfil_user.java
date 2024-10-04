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

import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;

public class Perfil_user extends AppCompatActivity {
    private TextView usernameTextView, emailTextView;
    private Switch notificationSwitch;
    private Button logoutButton;
    private ImageView bellIcon;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfil_user);
        mAuth = FirebaseAuth.getInstance();
        usernameTextView = findViewById(R.id.usernameValue);
        emailTextView = findViewById(R.id.emailValue);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        logoutButton = findViewById(R.id.logoutButton);
        bellIcon = findViewById(R.id.imageView4);

        // Aquí deberías cargar los datos del usuario actual
        usernameTextView.setText("Usuario");
        emailTextView.setText("admin@hotmail.com");

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implementar lógica de cierre de sesión
                Toast.makeText(Perfil_user.this, "Cerrando sesión...", Toast.LENGTH_SHORT).show();
                logout();
                // Aquí deberías navegar de vuelta a la pantalla de inicio de sesión
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
    private void logout() {
        mAuth.signOut();
        Toast.makeText(Perfil_user.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Perfil_user.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    }
