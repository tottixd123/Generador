package com.example.gemerador.Admin;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ClipboardManager;
import android.content.ClipData;

import com.example.gemerador.Email.EmailConfig;
import com.example.gemerador.Email.EmailUtil;
import com.example.gemerador.Email.JavaMailAPI;
import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AutoCrearUsuario extends AppCompatActivity implements JavaMailAPI.OnEmailSentListener {
    private static final String TAG = "AutoCrearUsuario";
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference solicitudesRef;
    private TextView tvUserInfo;
    private TextView tvPassword;
    private Button btnCopiarPassword;
    private Button btnFinalizar;
    private String generatedPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_crear_usuario);

        initializeViews();
        initializeFirebase();

        Solicitud solicitud = getSolicitudFromIntent();
        if (solicitud == null) {
            handleError("No se recibieron datos de la solicitud");
            return;
        }

        mostrarInformacionUsuario(solicitud);
        generatedPassword = generarPassword();
        procesarCreacionUsuario(solicitud);
    }

    private void enviarCorreoCredenciales(Solicitud solicitud) {
        String messageBody = EmailUtil.createWelcomeEmailBody(
                solicitud.getNombre(),
                solicitud.getEmail(),
                generatedPassword
        );

        JavaMailAPI javaMailAPI = new JavaMailAPI(
                this,
                solicitud.getEmail(),
                EmailConfig.WELCOME_SUBJECT,
                messageBody,
                this
        );
        javaMailAPI.sendEmail();
    }

    @Override
    public void onEmailSent(boolean success, String message) {
        runOnUiThread(() -> {
            if (success) {
                Toast.makeText(this,
                        "Se han enviado las credenciales al correo registrado",
                        Toast.LENGTH_LONG).show();
            } else {
                String errorMessage = "No se pudo enviar el correo con las credenciales";
                if (message != null && !message.isEmpty()) {
                    errorMessage += ": " + message;
                    Log.e(TAG, "Error sending email: " + message);
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");
            solicitudesRef = FirebaseDatabase.getInstance().getReference("solicitudes_registro");
        } catch (Exception e) {
            Log.e("AutoCrearUsuario", "Error al inicializar Firebase: " + e.getMessage());
            handleError("Error al conectar con la base de datos");
        }
    }

    private Solicitud getSolicitudFromIntent() {
        try {
            Solicitud solicitud = (Solicitud) getIntent().getSerializableExtra(GestionSolicitudesActivity.EXTRA_SOLICITUD);
            if (solicitud == null) {
                Log.e(TAG, "Solicitud data is null");
            }
            return solicitud;
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener solicitud: " + e.getMessage());
            return null;
        }
    }

    private void handleError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e("AutoCrearUsuario", message);
        finish();
    }

    private void initializeViews() {
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvPassword = findViewById(R.id.tvPassword);
        btnCopiarPassword = findViewById(R.id.btnCopiarPassword);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        btnCopiarPassword.setOnClickListener(v -> copiarPassword());
        btnFinalizar.setOnClickListener(v -> {
            Toast.makeText(this,
                    "Finalizando proceso y enviando credenciales...",
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void mostrarInformacionUsuario(Solicitud solicitud) {
        String infoUsuario = "Información del nuevo usuario:\n\n" +
                "Nombre: " + solicitud.getNombre() + "\n" +
                "Email: " + solicitud.getEmail() + "\n" +
                "Área: " + solicitud.getArea() + "\n" +
                "Cargo: " + solicitud.getCargo();
        tvUserInfo.setText(infoUsuario);
    }

    private String generarPassword() {
        String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        password.append(CARACTERES.charAt(random.nextInt(26)));
        password.append(random.nextInt(10));
        password.append(CARACTERES.substring(62).charAt(random.nextInt(8)));

        while (password.length() < 12) {
            password.append(CARACTERES.charAt(random.nextInt(CARACTERES.length())));
        }

        char[] characters = password.toString().toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }

        return new String(characters);
    }

    private void procesarCreacionUsuario(Solicitud solicitud) {
        mAuth.createUserWithEmailAndPassword(solicitud.getEmail(), generatedPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        guardarInformacionUsuario(userId, solicitud);
                        tvPassword.setText("Contraseña generada: " + generatedPassword);
                        btnCopiarPassword.setVisibility(View.VISIBLE);
                        enviarCorreoCredenciales(solicitud);
                    } else {
                        Toast.makeText(AutoCrearUsuario.this,
                                "Error al crear la cuenta: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
    }

    private void guardarInformacionUsuario(String userId, Solicitud solicitud) {
        Map<String, Object> user = new HashMap<>();
        user.put("nombre", solicitud.getNombre());
        user.put("email", solicitud.getEmail());
        user.put("area", solicitud.getArea());
        user.put("cargo", solicitud.getCargo());
        user.put("role", "Usuario");
        user.put("esAdmin", false);

        mDatabase.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    actualizarEstadoSolicitud(solicitud.getId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AutoCrearUsuario.this,
                            "Error al guardar información: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void actualizarEstadoSolicitud(String solicitudId) {
        solicitudesRef.child(solicitudId).child("estado").setValue("aprobado")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AutoCrearUsuario.this,
                            "Usuario creado exitosamente",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AutoCrearUsuario.this,
                            "Error al actualizar solicitud: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void copiarPassword() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("password", generatedPassword);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Contraseña copiada al portapapeles", Toast.LENGTH_SHORT).show();
    }
}