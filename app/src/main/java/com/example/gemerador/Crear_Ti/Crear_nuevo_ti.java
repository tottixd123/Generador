package com.example.gemerador.Crear_Ti;

import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.Notificaciones.TicketNotification;
import com.example.gemerador.Notificaciones.TicketNotificationAdapter;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    private static final String PREFS_NAME = "TicketPrefs";
    private static final String TICKET_COUNTER_KEY = "ticketCounter";
    private static final int PICK_IMAGE_REQUEST = 1;
    private SharedPreferences sharedPreferences;
    private TextView ticketCounterTextView;
    private Spinner problemSpinner, areaSpinner;
    private EditText problemDetailEditText;
    private Button selectImageButton, sendTicketButton;
    private ImageView selectedImageView;
    private Uri selectedImageUri;
    private int ticketCounter = 0;
    private Spinner prioritySpinner;
    private ImageView backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_nuevo_ti);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Recuperar el último número de ticket usado
        ticketCounter = sharedPreferences.getInt(TICKET_COUNTER_KEY, 0);

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

        // Configurar el Spinner de prioridad
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_options, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);
    }

    private void initializeViews() {
        ticketCounterTextView = findViewById(R.id.ticketCounterTextView);
        problemSpinner = findViewById(R.id.problemSpinner);
        areaSpinner = findViewById(R.id.areaSpinner);
        problemDetailEditText = findViewById(R.id.problemDetailEditText);
        selectImageButton = findViewById(R.id.selectImageButton);
        sendTicketButton = findViewById(R.id.sendTicketButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        selectImageButton.setOnClickListener(v -> openImageChooser());
        sendTicketButton.setOnClickListener(v -> sendTicket());
        backButton.setOnClickListener(v -> onBackPressed());
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
        String priority = prioritySpinner.getSelectedItem().toString();
        String ticketNumber = "Ticket-C" + String.format("%03d", ticketCounter);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Error: Usuario no identificado", Toast.LENGTH_SHORT).show();
            return;
        }

        ticketCounter++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(TICKET_COUNTER_KEY, ticketCounter);
        editor.apply();

        if (problem.isEmpty() || area.isEmpty() || details.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear el objeto JSON para MockAPI
        JSONObject ticketJson = createTicketJson(ticketNumber, problem, area, details, priority, currentUser);

        // Enviar a MockAPI
        sendTicketToMockAPI(ticketJson, new TicketCallback() {
            @Override
            public void onSuccess(String ticketId) {
                // Después de éxito en MockAPI, guardar en Firebase
                saveTicketReferenceToFirebase(ticketId, ticketJson);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(Crear_nuevo_ti.this,
                        "Error al enviar el ticket: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private JSONObject createTicketJson(String ticketNumber, String problem, String area,
                                        String details, String priority, FirebaseUser currentUser) {
        JSONObject ticketJson = new JSONObject();
        try {
            ticketJson.put("ticketNumber", ticketNumber);
            ticketJson.put("problemSpinner", problem);
            ticketJson.put("area_problema", area);
            ticketJson.put("detalle", details);
            ticketJson.put("imagen", selectedImageUri != null ? selectedImageUri.toString() : "");
            ticketJson.put("username", currentUser.getDisplayName() != null ?
                    currentUser.getDisplayName() : currentUser.getEmail());
            ticketJson.put("priority", priority);
            ticketJson.put("status", Ticket.STATUS_PENDING);
            ticketJson.put("creationDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ticketJson;
    }
    private void sendTicketToMockAPI(JSONObject ticketJson, TicketCallback callback) {
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
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String ticketId = jsonResponse.getString("id");
                        callback.onSuccess(ticketId);
                    } catch (JSONException e) {
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                } else {
                    callback.onError(response.body().string());
                }
            }
        });
    }

    private void saveTicketReferenceToFirebase(String ticketId, JSONObject ticketJson) {
        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference()
                .child("tickets")
                .child(ticketId);

        try {
            Map<String, Object> ticketMap = new HashMap<>();
            ticketMap.put("mockApiId", ticketId);
            ticketMap.put("ticketNumber", ticketJson.getString("ticketNumber"));
            ticketMap.put("status", ticketJson.getString("status"));
            ticketMap.put("priority", ticketJson.getString("priority"));
            ticketMap.put("creationDate", ticketJson.getString("creationDate"));
            ticketMap.put("assignedWorkerId", "");

            ticketsRef.setValue(ticketMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Crear_nuevo_ti.this,
                                "Ticket creado exitosamente", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Crear_nuevo_ti.this, Inicio_User.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Crear_nuevo_ti.this,
                                "Error al guardar en Firebase: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    interface TicketCallback {
        void onSuccess(String ticketId);
        void onError(String error);
    }

    private void updateTicketCounter() {
        ticketCounterTextView.setText("Ticket-C" + String.format("%03d", ticketCounter));
    }
}