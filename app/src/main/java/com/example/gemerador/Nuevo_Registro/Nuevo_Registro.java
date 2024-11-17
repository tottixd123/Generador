package com.example.gemerador.Nuevo_Registro;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Models.Solicitud;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class Nuevo_Registro extends AppCompatActivity {
    private EditText etNombreCompleto, etEmail, etNumeroContacto, editotroCargo;
    private Spinner spinnerArea, spinnerCargo;
    private Button btnEnviarSolicitud;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean isProcessing = false;
    // Lista de áreas de la Dirección de Agricultura Cajamarca
    private final List<String> areas = Arrays.asList(
            "Direccion Principal de Agricultura",
            "Dirección de Competitividad Agraria",
            "Dirección de Estadística e Información Agraria",
            "Dirección de Infraestructura Agraria",
            "Recursos Humanos",
            "Tesoreia",
            "Contabilidad"
    );
    // Lista de cargos
    private final List<String> cargos = Arrays.asList(
            "Jefe de Area",
            "Secretario/a",
            "Otro"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nuevo_registro);

        // Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Inicializar Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference().child("solicitudes_registro");

        // Inicializar vistas
        initializeViews();
        setupSpinners();

        // Configurar el botón de regreso
        findViewById(R.id.atras_soli).setOnClickListener(v -> finish());

        // Configurar el botón de enviar
        btnEnviarSolicitud.setOnClickListener(v -> enviarSolicitud());
    }

    private void initializeViews() {
        etNombreCompleto = findViewById(R.id.etNombreCompleto);
        etEmail = findViewById(R.id.etEmail);
        spinnerArea = findViewById(R.id.spinnerArea);
        spinnerCargo = findViewById(R.id.spinnerCargo);
        editotroCargo=findViewById(R.id.etOtroCargo);
        etNumeroContacto = findViewById(R.id.etNumeroContacto);
        btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
    }
    private void setupSpinners() {
        // Configurar Spinner de Áreas
        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                areas
        );
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArea.setAdapter(areaAdapter);

        // Configurar Spinner de Cargos
        ArrayAdapter<String> cargoAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                cargos
        );
        cargoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCargo.setAdapter(cargoAdapter);

        spinnerCargo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selecionarOpcion = parent.getItemAtPosition(position).toString();

                // Muestra el campo de texto si "Otro" está seleccionado
                if (selecionarOpcion.equals("Otro")) {
                    editotroCargo.setVisibility(View.VISIBLE);
                } else {
                    editotroCargo.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No se requiere implementación
            }
        });


    }

    private void enviarSolicitud() {
        Log.d("Solicitud", "Iniciando proceso de envío de solicitud");
        if (isProcessing) return;
        isProcessing = true;
        btnEnviarSolicitud.setEnabled(false);

        // Obtener y validar los datos
        String nombreCompleto = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String area = spinnerArea.getSelectedItem().toString();
        String cargo = spinnerCargo.getSelectedItem().toString();
        String otroCargos = editotroCargo.getText().toString().trim();
        String numeroContacto = etNumeroContacto.getText().toString().trim();

        // Validación de campos
        if (!validarCampos(nombreCompleto, email, numeroContacto)) {
            isProcessing = false;
            btnEnviarSolicitud.setEnabled(true);
            return;
        }

        // Crear objeto Solicitud
        Solicitud nuevaSolicitud = new Solicitud(
                null,
                nombreCompleto,
                email,
                area,
                cargo,
                otroCargos,
                numeroContacto,
                "pendiente"
        );

        // Guardar directamente sin autenticación
        guardarSolicitud(nuevaSolicitud);
    }
    private void guardarSolicitud(Solicitud nuevaSolicitud) {
        Log.d("Solicitud", "Intentando guardar solicitud en Firebase");
        // Crear una referencia directa a la colección de solicitudes
        DatabaseReference solicitudesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("solicitudes_registro");

        // Generar una nueva key para la solicitud
        String solicitudId = solicitudesRef.push().getKey();

        if (solicitudId != null) {
            nuevaSolicitud.setId(solicitudId);

            // Intentar guardar la solicitud
            solicitudesRef.child(solicitudId)
                    .setValue(nuevaSolicitud)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Firebase", "Solicitud guardada exitosamente");
                        Toast.makeText(Nuevo_Registro.this,
                                "Solicitud de registro enviada exitosamente",
                                Toast.LENGTH_SHORT).show();
                        limpiarCampos();
                        isProcessing = false;
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firebase", "Error al guardar solicitud", e);
                        manejarError("Error al guardar la solicitud: " + e.getMessage());
                        isProcessing = false;
                        btnEnviarSolicitud.setEnabled(true);
                    });
        } else {
            manejarError("Error al generar ID de solicitud");
            isProcessing = false;
            btnEnviarSolicitud.setEnabled(true);
        }
    }

    private boolean validarCampos(String nombreCompleto, String email, String numeroContacto) {
        if (nombreCompleto.isEmpty() || email.isEmpty() || numeroContacto.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Por favor ingrese un email válido");
            return false;
        }

        if (!numeroContacto.matches("\\d{8,}")) {
            etNumeroContacto.setError("Ingrese un número válido (mínimo 8 dígitos)");
            return false;
        }

        return true;
    }

    private void manejarError(String mensaje) {
        Log.e("Error", mensaje);
        Toast.makeText(Nuevo_Registro.this, mensaje, Toast.LENGTH_LONG).show();
        isProcessing = false;
        btnEnviarSolicitud.setEnabled(true);
    }

    private void limpiarCampos() {
        etNombreCompleto.setText("");
        etEmail.setText("");
        spinnerArea.setSelection(0);
        spinnerCargo.setSelection(0);
        etNumeroContacto.setText("");
        btnEnviarSolicitud.setEnabled(true);
    }
}