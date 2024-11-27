package com.example.gemerador.Crear_Ti;


import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
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
        fetchLastTicketNumber();
        setupSpinners();
        setupListeners();
    }

    private void initializeComponents() {
        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

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
    private void fetchLastTicketNumber() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Get reference to the tickets in Firebase/MockAPI
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // En caso de fallo, usar el valor guardado en SharedPreferences
                runOnUiThread(() -> {
                    ticketCounter = sharedPreferences.getInt(TICKET_COUNTER_KEY, 0);
                    updateTicketCounter();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        // Parse the JSON array and find the highest ticket number
                        org.json.JSONArray tickets = new org.json.JSONArray(jsonData);
                        int highestNumber = 0;

                        for (int i = 0; i < tickets.length(); i++) {
                            JSONObject ticket = tickets.getJSONObject(i);
                            String ticketNumber = ticket.getString("ticketNumber");
                            if (ticketNumber.startsWith("Ticket-C")) {
                                try {
                                    int number = Integer.parseInt(ticketNumber.substring(8)); // Extract number after "Ticket-C"
                                    highestNumber = Math.max(highestNumber, number);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        // Update ticket counter to be one more than the highest existing number
                        final int nextNumber = highestNumber + 1;
                        runOnUiThread(() -> {
                            ticketCounter = nextNumber;
                            updateTicketCounter();
                            // Save to SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt(TICKET_COUNTER_KEY, ticketCounter);
                            editor.apply();
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Fallback to SharedPreferences value
                        runOnUiThread(() -> {
                            ticketCounter = sharedPreferences.getInt(TICKET_COUNTER_KEY, 0);
                            updateTicketCounter();
                        });
                    }
                }
            }
        });
    }

    private void setupSpinners() {
        setupSpinner(problemSpinner, R.array.problem_options, "Seleccione un problema");
        setupSpinner(areaSpinner, R.array.area_options, "Seleccione un área");
        setupSpinner(prioritySpinner, R.array.priority_options, "Seleccione prioridad");
    }

    private void setupSpinner(Spinner spinner, int arrayResourceId, String hint) {
        String[] originalArray = getResources().getStringArray(arrayResourceId);
        String[] arrayWithHint = new String[originalArray.length + 1];
        arrayWithHint[0] = hint;
        System.arraycopy(originalArray, 0, arrayWithHint, 1, originalArray.length);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayWithHint) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;

                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // Establecer "Pendiente" como estado por defecto para el Spinner de prioridad
        if (arrayResourceId == R.array.priority_options) {
            spinner.setSelection(1); // Selecciona "Media" por defecto
        }
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
                // Procesar la imagen seleccionada
                processSelectedImage(selectedImageUri);
            } catch (IOException e) {
                showError("Error al procesar la imagen: " + e.getMessage());
            }
        }
    }
    //  método para procesar imágenes
    private void processSelectedImage(Uri imageUri) throws IOException {
        // Obtener el bitmap original
        Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

        // Comprimir la imagen
        Bitmap compressedBitmap = compressImage(originalBitmap);

        // Convertir a Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] imageBytes = baos.toByteArray();

        // Verificar el tamaño
        if (imageBytes.length > MAX_IMAGE_SIZE) {
            showError("La imagen es demasiado grande. Máximo 1MB.");
            return;
        }

        // Guardar la imagen codificada
        encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

        // Mostrar la vista previa
        selectedImageView.setImageBitmap(compressedBitmap);
        selectedImageView.setVisibility(View.VISIBLE);
    }
    private Bitmap compressImage(Bitmap original) {
        int maxDimension = 800; // Máximo 800px en cualquier dimensión
        float ratio = Math.min(
                (float) maxDimension / original.getWidth(),
                (float) maxDimension / original.getHeight()
        );

        int newWidth = Math.round(original.getWidth() * ratio);
        int newHeight = Math.round(original.getHeight() * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
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
        if (problemSpinner.getSelectedItemPosition() == 0) {
            showError("Por favor, seleccione un tipo de problema");
            return false;
        }

        if (areaSpinner.getSelectedItemPosition() == 0) {
            showError("Por favor, seleccione un área");
            return false;
        }

        String details = problemDetailEditText.getText().toString().trim();
        if (details.isEmpty()) {
            showError("Por favor, ingrese los detalles del problema");
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
                        try {
                            String ticketNumber = ticketJson.getString("ticketNumber");
                            String message = "Su ticket #" + ticketNumber + " ha sido creado exitosamente.";
                            sendConfirmationNotification(ticketNumber, message);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
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
    private void sendConfirmationNotification(String ticketNumber, String message) {
        String priority = prioritySpinner.getSelectedItem().toString();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String username = currentUser != null ?
                (currentUser.getEmail() != null ? currentUser.getEmail() : "Usuario") : "Sistema";
        showSystemNotification(ticketNumber, message);
        SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        try {
            String savedNotifications = prefs.getString("notifications", "[]");
            JSONArray notificationsArray = new JSONArray(savedNotifications);
            JSONObject newNotification = new JSONObject();
            newNotification.put("ticketId", ticketNumber);
            newNotification.put("status", "Creado");
            newNotification.put("message", message);
            newNotification.put("timestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                    Locale.getDefault()).format(new Date()));
            newNotification.put("priority", priority);
            newNotification.put("username", username);
            JSONArray updatedArray = new JSONArray();
            updatedArray.put(newNotification);
            for (int i = 0; i < notificationsArray.length(); i++) {
                updatedArray.put(notificationsArray.get(i));
            }
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("notifications", updatedArray.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(this, Inicio_User.class);
        intent.putExtra("showNotification", true);
        startActivity(intent);
        finish();
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
    private void showSystemNotification(String ticketNumber, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ticket_channel")
                .setSmallIcon(R.drawable.gata)
                .setContentTitle("Nuevo Ticket Creado")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        Intent intent = new Intent(this, Inicio_User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(Integer.parseInt(ticketNumber.replaceAll("\\D+","")), builder.build());
        } else {
            Log.e("Notification", "No permission to post notifications");
        }
    }
}