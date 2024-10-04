package com.example.gemerador.Crear_Ti;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

public class Crear_nuevo_ti extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextView ticketCounterTextView;
    private Spinner problemSpinner, areaSpinner;
    private EditText problemDetailEditText;
    private Button selectImageButton, sendTicketButton;
    private ImageView selectedImageView;
    private Uri selectedImageUri;
    private int ticketCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_nuevo_ti);

        initializeViews();
        setupListeners();
        updateTicketCounter();
        setupSpinners();
    }
    private void setupSpinners() {
        // Configurar el Spinner de problemas
        ArrayAdapter<CharSequence> problemAdapter = ArrayAdapter.createFromResource(this,
                R.array.problem_options, android.R.layout.simple_spinner_item);
        problemAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        problemSpinner.setAdapter(problemAdapter);

        // Configurar el Spinner de áreas
        ArrayAdapter<CharSequence> areaAdapter = ArrayAdapter.createFromResource(this,
                R.array.area_options, android.R.layout.simple_spinner_item);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setAdapter(areaAdapter);
    }

    private void initializeViews() {
        ticketCounterTextView = findViewById(R.id.ticketCounterTextView);
        problemSpinner = findViewById(R.id.problemSpinner);
        areaSpinner = findViewById(R.id.areaSpinner);
        problemDetailEditText = findViewById(R.id.problemDetailEditText);
        selectImageButton = findViewById(R.id.selectImageButton);
        sendTicketButton = findViewById(R.id.sendTicketButton);
        selectedImageView = findViewById(R.id.selectedImageView);
    }

    private void setupListeners() {
        selectImageButton.setOnClickListener(v -> openImageChooser());
        sendTicketButton.setOnClickListener(v -> sendTicket());
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                selectedImageView.setImageBitmap(MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri));
                selectedImageView.setVisibility(ImageView.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void sendTicket() {
        String problem = problemSpinner.getSelectedItem().toString();
        String area = areaSpinner.getSelectedItem().toString();
        String details = problemDetailEditText.getText().toString();

        // Verificar que todos los campos estén llenos
        if (problem.isEmpty() || area.isEmpty() || details.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject ticketJson = new JSONObject();
        try {
            ticketJson.put("ticketNumber", "Ticket-C" + String.format("%03d", ticketCounter));
            ticketJson.put("problemSpinner", problem);
            ticketJson.put("area_problema", area);
            ticketJson.put("detalle", details);
            ticketJson.put("imagen", selectedImageUri != null ? selectedImageUri.toString() : "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, ticketJson.toString());
        Request request = new Request.Builder()
                .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(Crear_nuevo_ti.this, "Error al enviar el ticket: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(Crear_nuevo_ti.this, "Ticket enviado con éxito", Toast.LENGTH_SHORT).show();
                        ticketCounter++;
                        updateTicketCounter();
                        Intent intent = new Intent(Crear_nuevo_ti.this, Inicio_User.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    String responseBody = response.body().string();
                    runOnUiThread(() -> Toast.makeText(Crear_nuevo_ti.this, "Error al enviar el ticket: " + responseBody, Toast.LENGTH_LONG).show());
                }
            }
        });
    }
    private void updateTicketCounter() {
        ticketCounterTextView.setText("Ticket-C" + String.format("%03d", ticketCounter));
    }
}