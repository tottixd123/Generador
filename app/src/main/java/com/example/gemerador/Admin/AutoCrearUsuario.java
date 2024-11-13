package com.example.gemerador.Admin;

import android.content.Context;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


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
import com.google.firebase.database.ServerValue;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AutoCrearUsuario extends AppCompatActivity implements JavaMailAPI.OnEmailSentListener {
    private static final String TAG = "AutoCrearUsuario";
    private static final int MIN_PASSWORD_LENGTH = 12;
    public static final String EXTRA_SOLICITUD = "solicitudes_registro";

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference solicitudesRef;
    private TextView tvUserInfo;
    private TextView tvPassword;
    private Button btnCopiarPassword;
    private Button btnFinalizar;
    private ProgressBar progressBar;

    private String generatedPassword;
    private Solicitud solicitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_crear_usuario);

        initializeViews();
        initializeFirebase();

        // Obtener el ID de la solicitud del intent
        String solicitudId = getIntent().getStringExtra(EXTRA_SOLICITUD);
        if (solicitudId == null) {
            handleError("No se recibió el ID de la solicitud");
            return;
        }

        // Cargar la solicitud desde Firebase
        solicitudesRef.child(solicitudId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                solicitud = snapshot.getValue(Solicitud.class);
                if (solicitud != null) {
                    solicitud.setId(snapshot.getKey());
                    if (solicitud.isValid()) {
                        mostrarInformacionUsuario(solicitud);
                        generatedPassword = generarPassword();
                        procesarCreacionUsuario(solicitud);
                    } else {
                        handleError("La solicitud no contiene todos los datos necesarios");
                    }
                } else {
                    handleError("Error al convertir los datos de la solicitud");
                }
            } else {
                handleError("No se encontró la solicitud en la base de datos");
            }
        }).addOnFailureListener(e -> handleError("Error al obtener la solicitud: " + e.getMessage()));
    }
    private void mostrarInformacionUsuario(Solicitud solicitud) {
        if (solicitud != null && tvUserInfo != null) {
            StringBuilder info = new StringBuilder();
            info.append("Información del usuario:\n\n");
            info.append("Nombre: ").append(solicitud.getNombre()).append("\n");
            info.append("Email: ").append(solicitud.getEmail()).append("\n");
            info.append("Área: ").append(solicitud.getArea()).append("\n");
            info.append("Cargo: ").append(solicitud.getCargo());

            tvUserInfo.setText(info.toString());
        }
    }
    // Método para manejar la visibilidad
    private void setVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    // Método actualizado showLoading
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnFinalizar != null) {
            btnFinalizar.setEnabled(!show);
        }
    }
    private void initializeViews() {
        tvUserInfo = findViewById(R.id.tvUserInfo);
        tvPassword = findViewById(R.id.tvPassword);
        btnCopiarPassword = findViewById(R.id.btnCopiarPassword);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        progressBar = findViewById(R.id.progressBar);

        btnCopiarPassword.setOnClickListener(v -> copiarPassword());
        btnFinalizar.setOnClickListener(v -> finalizarProceso());

        // Inicialmente ocultar contraseña y botón de copiar
        tvPassword.setVisibility(View.GONE);
        btnCopiarPassword.setVisibility(View.GONE);
    }

    private void initializeFirebase() {
        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");
            solicitudesRef = FirebaseDatabase.getInstance().getReference("solicitudes_registro");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar Firebase: " + e.getMessage());
            handleError("Error al conectar con la base de datos");
        }
    }
    private void procesarCreacionUsuario(Solicitud solicitud) {
        showLoading(true);
        mAuth.createUserWithEmailAndPassword(solicitud.getEmail(), generatedPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = task.getResult().getUser().getUid();
                        guardarInformacionUsuario(userId, solicitud);
                    } else {
                        handleError(task.getException());
                    }
                });
    }
    private String generarPassword() {
        String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
        String NUMEROS = "0123456789";
        String ESPECIALES = "!@#$%^&*";

        StringBuilder password = new StringBuilder();
        Random random = new SecureRandom(); // Usar SecureRandom para mayor seguridad

        // Asegurar al menos un carácter de cada tipo
        password.append(MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length())));
        password.append(MINUSCULAS.charAt(random.nextInt(MINUSCULAS.length())));
        password.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        password.append(ESPECIALES.charAt(random.nextInt(ESPECIALES.length())));

        // Completar el resto de la contraseña
        String TODOS_CARACTERES = MAYUSCULAS + MINUSCULAS + NUMEROS + ESPECIALES;
        while (password.length() < MIN_PASSWORD_LENGTH) {
            password.append(TODOS_CARACTERES.charAt(random.nextInt(TODOS_CARACTERES.length())));
        }

        // Mezclar la contraseña
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
    private void guardarInformacionUsuario(String userId, Solicitud solicitud) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", solicitud.getNombre());
        userData.put("email", solicitud.getEmail());
        userData.put("area", solicitud.getArea());
        userData.put("cargo", solicitud.getCargo());
        userData.put("role", "Usuario");
        userData.put("esAdmin", false);
        userData.put("estado", "activo");
        userData.put("fechaCreacion", ServerValue.TIMESTAMP);

        mDatabase.child(userId).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    actualizarEstadoSolicitud(solicitud.getId(), userId);
                    mostrarCredenciales();
                    enviarCorreoCredenciales(solicitud);
                })
                .addOnFailureListener(this::handleError);
    }
    private void actualizarEstadoSolicitud(String solicitudId, String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", "aprobado");
        updates.put("usuarioId", userId);
        updates.put("fechaAprobacion", ServerValue.TIMESTAMP);

        solicitudesRef.child(solicitudId).updateChildren(updates)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al actualizar estado de solicitud: " + e.getMessage()));
    }

    private void mostrarCredenciales() {
        showLoading(false);
        tvPassword.setText("Contraseña generada: " + generatedPassword);
        tvPassword.setVisibility(View.VISIBLE);
        btnCopiarPassword.setVisibility(View.VISIBLE);
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
                if (!TextUtils.isEmpty(message)) {
                    errorMessage += ": " + message;
                    Log.e(TAG, "Error sending email: " + message);
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    private void copiarPassword() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("password", generatedPassword);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Contraseña copiada al portapapeles", Toast.LENGTH_SHORT).show();
    }

    private void finalizarProceso() {
        Toast.makeText(this, "Proceso finalizado exitosamente", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }
    private void handleError(Exception e) {
        showLoading(false);
        String mensaje = "Error al procesar la solicitud";

        if (e.getMessage().contains("email address is already in use")) {
            mensaje = "El correo electrónico ya está registrado";
        } else if (e.getMessage().contains("network error")) {
            mensaje = "Error de conexión. Por favor, verifica tu conexión a internet";
        }

        Log.e(TAG, "Error: " + e.getMessage());
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void handleError(String message) {
        showLoading(false);
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        setResult(RESULT_CANCELED);
        finish();
    }
}