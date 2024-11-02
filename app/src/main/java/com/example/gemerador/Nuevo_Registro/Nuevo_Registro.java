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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

public class Nuevo_Registro extends AppCompatActivity {
    private EditText etNombreCompleto, etEmail, etNumeroContacto, editotroCargo;
    private Spinner spinnerArea, spinnerCargo;
    private Button btnEnviarSolicitud;
    private DatabaseReference mDatabase;
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
            "Jefe",
            "Personal de Apoyo",
            "Otro"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nuevo_registro);

        // Inicializar Firebase Database con la referencia específica
        mDatabase = FirebaseDatabase.getInstance().getReference("solicitudes_registro");

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
        // Mostrar progreso
        btnEnviarSolicitud.setEnabled(false);

        // Obtener y validar los datos
        String nombreCompleto = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String area = spinnerArea.getSelectedItem().toString();
        String cargo = spinnerCargo.getSelectedItem().toString();
        String otroCargos = editotroCargo.getText().toString().trim();
        String numeroContacto = etNumeroContacto.getText().toString().trim();


        // Validación de campos vacíos
        if (nombreCompleto.isEmpty() || email.isEmpty() || numeroContacto.isEmpty()) {
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
                otroCargos,
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
                        finish();
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
        spinnerArea.setSelection(0);
        spinnerCargo.setSelection(0);
        etNumeroContacto.setText("");
        btnEnviarSolicitud.setEnabled(true);
    }
}