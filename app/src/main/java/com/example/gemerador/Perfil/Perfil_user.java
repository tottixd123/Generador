package com.example.gemerador.Perfil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Admin.AdminMenu;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.MainActivity;
import com.example.gemerador.R;
import com.example.gemerador.User_Admin.AdminUserManager;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Perfil_user extends AppCompatActivity {
    private TextView usernameTextView, emailTextView;
    private Switch notificationSwitch;
    private Button logoutButton, iniciobtn,changePasswordButton,regeUser;
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
        changePasswordButton = findViewById(R.id.changePasswordButton);
        bellIcon = findViewById(R.id.imageView4);
        regeUser = findViewById(R.id.btnpUser);
        regeUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Perfil_user.this, Inicio_User.class);
                startActivity(intent);
            }
        });

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

        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());

        bellIcon.setOnClickListener(v -> {
            notificationSwitch.setChecked(!notificationSwitch.isChecked());
            updateNotificationPreference(notificationSwitch.isChecked());
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateNotificationPreference(isChecked);
        });
    }
    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordEt = dialogView.findViewById(R.id.currentPasswordEt);
        EditText newPasswordEt = dialogView.findViewById(R.id.newPasswordEt);
        EditText confirmPasswordEt = dialogView.findViewById(R.id.confirmPasswordEt);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("")
                .setView(dialogView)
                .setPositiveButton("Editar Contraseña", null)
                .setNegativeButton("Cancelar", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String currentPassword = currentPasswordEt.getText().toString();
                String newPassword = newPasswordEt.getText().toString();
                String confirmPassword = confirmPasswordEt.getText().toString();

                if (validatePasswordInput(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword, dialog);
                }
            });
        });

        dialog.show();
    }

    private boolean validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "La nueva contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void changePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Reautenticar al usuario
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Actualizar la contraseña
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(Perfil_user.this,
                                    "Contraseña actualizada exitosamente", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(Perfil_user.this,
                                    "Error al actualizar la contraseña: " + updateTask.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    Toast.makeText(Perfil_user.this,
                            "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
                }
            });
        }
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