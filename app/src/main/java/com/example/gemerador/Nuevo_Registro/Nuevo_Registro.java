package com.example.gemerador.Nuevo_Registro;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Nuevo_Registro extends AppCompatActivity {
    private EditText etNombreCompleto, etEmail, etArea, etCargo, etNumeroContacto;
    private Button btnEnviarSolicitud;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nuevo_registro);

        // Inicializar Firebase Database con la referencia específica
        mDatabase = FirebaseDatabase.getInstance().getReference("solicitudes_registro");

        // Inicializar vistas
        initializeViews();

        // Configurar el botón de regreso
        findViewById(R.id.atras_soli).setOnClickListener(v -> finish());

        // Configurar el botón de enviar
        btnEnviarSolicitud.setOnClickListener(v -> enviarSolicitud());
    }

    private void initializeViews() {
        etNombreCompleto = findViewById(R.id.etNombreCompleto);
        etEmail = findViewById(R.id.etEmail);
        etArea = findViewById(R.id.etArea);
        etCargo = findViewById(R.id.etCargo);
        etNumeroContacto = findViewById(R.id.etNumeroContacto);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
    }

    private void enviarSolicitud() {
        // Mostrar progreso
        btnEnviarSolicitud.setEnabled(false);

        // Obtener y validar los datos
        String nombreCompleto = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String cargo = etCargo.getText().toString().trim();
        String numeroContacto = etNumeroContacto.getText().toString().trim();

        // Validación de campos vacíos
        if (nombreCompleto.isEmpty() || email.isEmpty() || area.isEmpty() ||
                cargo.isEmpty() || numeroContacto.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            btnEnviarSolicitud.setEnabled(true);
            return;
        }

        // Validar formato de email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Por favor ingrese un email válido");
            btnEnviarSolicitud.setEnabled(true);
            return;
        }

        // Validar formato de número de contacto (solo números y mínimo 8 dígitos)
        if (!numeroContacto.matches("\\d{8,}")) {
            etNumeroContacto.setError("Ingrese un número válido (mínimo 8 dígitos)");
            btnEnviarSolicitud.setEnabled(true);
            return;
        }

        // Crear objeto Solicitud
        Solicitud nuevaSolicitud = new Solicitud(
                null, // el ID se asignará automáticamente
                nombreCompleto,
                email,
                area,
                cargo,
                numeroContacto,
                "pendiente" // estado inicial
        );

        // Generar key única para la solicitud
        String solicitudId = mDatabase.push().getKey();
        if (solicitudId != null) {
            nuevaSolicitud.setId(solicitudId);

            // Guardar en Firebase
            mDatabase.child(solicitudId)
                    .setValue(nuevaSolicitud)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Nuevo_Registro.this,
                                "Solicitud de registro enviada exitosamente",
                                Toast.LENGTH_SHORT).show();
                        limpiarCampos();
                        finish(); // Cerrar la actividad después de enviar
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Nuevo_Registro.this,
                                "Error al enviar la solicitud. Por favor, intente nuevamente",
                                Toast.LENGTH_LONG).show();
                        btnEnviarSolicitud.setEnabled(true);
                    });
        }
    }

    private void limpiarCampos() {
        etNombreCompleto.setText("");
        etEmail.setText("");
        etArea.setText("");
        etCargo.setText("");
        etNumeroContacto.setText("");
        btnEnviarSolicitud.setEnabled(true);
    }
}