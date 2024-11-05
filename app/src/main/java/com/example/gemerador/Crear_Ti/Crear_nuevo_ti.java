package com.example.gemerador.Crear_Ti;

import static android.opengl.ETC1.encodeImage;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private Spinner problemSpinner, areaSpinner, prioritySpinner;
    private EditText problemDetailEditText;
    private Button selectImageButton, sendTicketButton;
    private ImageView selectedImageView, backButton;
    private Uri selectedImageUri;
    private int ticketCounter = 0;
    private FirebaseAuth mAuth;
    private boolean isSubmitting = false;
    private static final int MAX_IMAGE_SIZE = 1024 * 1024; // 1MB
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_nuevo_ti);

        initializeComponents();
        setupSpinners();
        setupListeners();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        ticketCounter = sharedPreferences.getInt(TICKET_COUNTER_KEY, 0);

        // Initialize views
        ticketCounterTextView = findViewById(R.id.ticketCounterTextView);
        problemSpinner = findViewById(R.id.problemSpinner);
        areaSpinner = findViewById(R.id.areaSpinner);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        problemDetailEditText = findViewById(R.id.problemDetailEditText);
        selectImageButton = findViewById(R.id.selectImageButton);
        sendTicketButton = findViewById(R.id.sendTicketButton);
        selectedImageView = findViewById(R.id.selectedImageView);
        backButton = findViewById(R.id.backButton);

        updateTicketCounter();
    }

    private void setupSpinners() {
        setupSpinner(problemSpinner, R.array.problem_options);
        setupSpinner(areaSpinner, R.array.area_options);
        setupSpinner(prioritySpinner, R.array.priority_options);
    }

    private void setupSpinner(Spinner spinner, int arrayResourceId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayResourceId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupListeners() {
        selectImageButton.setOnClickListener(v -> openImageChooser());
        sendTicketButton.setOnClickListener(v -> {
            if (!isSubmitting) {
                sendTicket();
            }
        });
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
                // Compress and encode the image
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                bitmap = compressImage(bitmap);
                encodedImage = encodeImage(bitmap);

                // Display the compressed image
                selectedImageView.setImageBitmap(bitmap);
                selectedImageView.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                showError("Error al cargar la imagen: " + e.getMessage());
            }
        }
    }
    private Bitmap compressImage(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // Calculate new dimensions while maintaining aspect ratio
        float ratio = Math.min((float) 800 / width, (float) 800 / height);
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }
    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void sendTicket() {
        if (!validateInput()) {
            return;
        }

        isSubmitting = true;
        sendTicketButton.setEnabled(false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Error: Usuario no identificado");
            resetSubmitState();
            return;
        }

        String ticketNumber = "Ticket-C" + String.format("%03d", ticketCounter);

        // Obtener la referencia al usuario en Firebase Database
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());

        userRef.get().addOnCompleteListener(task -> {
            String userName;
            if (task.isSuccessful() && task.getResult() != null &&
                    task.getResult().child("name").getValue(String.class) != null) {
                userName = task.getResult().child("name").getValue(String.class);
            } else {
                // Usar email como fallback, o un ID parcial si no hay email
                userName = currentUser.getEmail() != null ?
                        currentUser.getEmail() :
                        "Usuario " + currentUser.getUid().substring(0, 5);
            }

            JSONObject ticketJson = createTicketJson(ticketNumber, currentUser, userName);
            sendTicketToMockAPI(ticketJson);
        });
    }

    private boolean validateInput() {
        String details = problemDetailEditText.getText().toString().trim();

        if (details.isEmpty()) {
            showError("Por favor, ingrese los detalles del problema");
            return false;
        }

        if (problemSpinner.getSelectedItemPosition() == 0) {
            showError("Por favor, seleccione un tipo de problema");
            return false;
        }

        if (areaSpinner.getSelectedItemPosition() == 0) {
            showError("Por favor, seleccione un área");
            return false;
        }

        return true;
    }

    private JSONObject createTicketJson(String ticketNumber, FirebaseUser currentUser, String userName) {
        JSONObject ticketJson = new JSONObject();
        try {
            ticketJson.put("ticketNumber", ticketNumber);
            ticketJson.put("problemSpinner", problemSpinner.getSelectedItem().toString());
            ticketJson.put("area_problema", areaSpinner.getSelectedItem().toString());
            ticketJson.put("detalle", problemDetailEditText.getText().toString().trim());
            ticketJson.put("imagen", encodedImage != null ? encodedImage : "");

            // Usar el nombre proporcionado
            ticketJson.put("createdBy", userName);
            ticketJson.put("userId", currentUser.getUid());
            ticketJson.put("priority", prioritySpinner.getSelectedItem().toString());
            ticketJson.put("status", "Pendiente");
            ticketJson.put("creationDate", getCurrentTimestamp());
            ticketJson.put("assignedWorkerId", "");
            ticketJson.put("assignedWorkerName", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ticketJson;
    }

    private void sendTicketToMockAPI(JSONObject ticketJson) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, ticketJson.toString());

        Request request = new Request.Builder()
                .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showError("Error de conexión: " + e.getMessage());
                    resetSubmitState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    incrementTicketCounter();
                    runOnUiThread(() -> {
                        Toast.makeText(Crear_nuevo_ti.this,
                                "Ticket creado exitosamente", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    });
                } else {
                    runOnUiThread(() -> {
                        showError("Error al crear ticket: " + response.message());
                        resetSubmitState();
                    });
                }
            }
        });
    }
    private void incrementTicketCounter() {
        ticketCounter++;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(TICKET_COUNTER_KEY, ticketCounter);
        editor.apply();
    }

    private void resetSubmitState() {
        isSubmitting = false;
        sendTicketButton.setEnabled(true);
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(Crear_nuevo_ti.this, Inicio_User.class);
        startActivity(intent);
        finish();
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private void updateTicketCounter() {
        ticketCounterTextView.setText("Ticket-C" + String.format("%03d", ticketCounter));
    }
}