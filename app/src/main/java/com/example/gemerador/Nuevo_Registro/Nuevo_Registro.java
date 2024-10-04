package com.example.gemerador.Nuevo_Registro;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gemerador.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Nuevo_Registro extends AppCompatActivity {
    private EditText etNombreCompleto, etEmail, etArea, etCargo, etNumeroContacto;
    private Button btnEnviarSolicitud;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_nuevo_registro);
        //inicializar firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("solicitud_registro");
        //Inicializar vistas
        etNombreCompleto = findViewById(R.id.etNombreCompleto);
        etEmail = findViewById(R.id.etEmail);
        etArea = findViewById(R.id.etArea);
        etCargo = findViewById(R.id.etCargo);
        etNumeroContacto = findViewById(R.id.etNumeroContacto);
        btnEnviarSolicitud=findViewById(R.id.btnEnviarSolicitud);

        Button btnEnviarSolicitud = findViewById(R.id.btnEnviarSolicitud);
        btnEnviarSolicitud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Lógica para enviar la solicitud de registro
                enviarSolicitud();
            }
        });
    }
    private void enviarSolicitud() {
        String nombreCompleto = etNombreCompleto.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String cargo = etCargo.getText().toString().trim();
        String numeroContacto = etNumeroContacto.getText().toString().trim();
        if (!nombreCompleto.isEmpty() && !email.isEmpty() && !area.isEmpty() && !cargo.isEmpty() && !numeroContacto.isEmpty()) {
            Toast.makeText(this,"Por favor, complete todos los campos",Toast.LENGTH_SHORT).show();
            return;
        }
        //Crear un objeto map con los datos de la solicitud
        Map<String, Object> solicitud = new HashMap<>();
        solicitud.put("nombreCompleto", nombreCompleto);
        solicitud.put("email", email);
        solicitud.put("area", area);
        solicitud.put("cargo", cargo);
        solicitud.put("numeroContacto", numeroContacto);
        solicitud.put("estado", "pendiente");

        //Generar una clave unica para la solicitud
        String solicitudId = mDatabase.push().getKey();

        //Guardar la solicitud en Firebase
        mDatabase.child(solicitudId).setValue(solicitud)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Nuevo_Registro.this, "Solicitud de registro enviada", Toast.LENGTH_SHORT).show();
                    limpiarCampos();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Nuevo_Registro.this, "Error al enviar la solicitud"+e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void limpiarCampos() {
        etNombreCompleto.setText("");
        etEmail.setText("");
        etArea.setText("");
        etCargo.setText("");
    }
}