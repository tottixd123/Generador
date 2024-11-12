package com.example.gemerador.Trabajador;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class TrabajadorTicketDetailActivity extends AppCompatActivity {
    private TextView tvTicketNumber, tvProblemType, tvArea, tvDetail, tvCreationDate;
    private TextView tvStatus, tvPriority, tvComments;
    private ImageView ivTicketImage;
    private EditText etNewComment;
    private Button btnUpdateStatus, btnAddComment;
    private Spinner spinnerStatus;
    private DatabaseReference mDatabase;
    private Ticket currentTicket;
    private String workerId;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajador_ticket_detail);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        initializeViews();
        loadTicketData();
    }
    private void initializeViews() {
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvProblemType = findViewById(R.id.tvProblemType);
        tvArea = findViewById(R.id.tvArea);
        tvDetail = findViewById(R.id.tvDetail);
        tvCreationDate = findViewById(R.id.tvCreationDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvPriority = findViewById(R.id.tvPriority);
        tvComments = findViewById(R.id.tvComments);
        ivTicketImage = findViewById(R.id.ivTicketImage);
        etNewComment = findViewById(R.id.etNewComment);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnAddComment = findViewById(R.id.btnAddComment);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        // Setup status spinner with valid statuses
        String[] statuses = {
                Ticket.STATUS_PENDING,
                Ticket.STATUS_IN_PROGRESS,
                Ticket.STATUS_COMPLETED
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        spinnerStatus.setAdapter(statusAdapter);

        btnUpdateStatus.setOnClickListener(v -> updateTicketStatus());
        btnAddComment.setOnClickListener(v -> addComment());
    }

    private void loadTicketData() {
        String ticketId = getIntent().getStringExtra("ticketId");
        if (ticketId == null) {
            Toast.makeText(this, "Error: ID de ticket no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador/" + ticketId)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(TrabajadorTicketDetailActivity.this,
                            "Error al cargar el ticket: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    try {
                        JSONObject jsonTicket = new JSONObject(jsonData);

                        final Ticket ticket = new Ticket(
                                jsonTicket.optString("ticketNumber"),
                                jsonTicket.optString("problemSpinner"),
                                jsonTicket.optString("area_problema"),
                                jsonTicket.optString("detalle"),
                                jsonTicket.optString("imagen"),
                                jsonTicket.optString("id"),
                                jsonTicket.optString("createdBy"),
                                jsonTicket.optString("creationDate"),
                                jsonTicket.optString("userId")
                        );

                        ticket.setStatus(jsonTicket.optString("status", "Pendiente"));
                        ticket.setPriority(jsonTicket.optString("priority", "Normal"));
                        ticket.setAssignedWorkerId(jsonTicket.optString("assignedWorkerId", ""));
                        ticket.setAssignedWorkerName(jsonTicket.optString("assignedWorkerName", ""));

                        runOnUiThread(() -> {
                            currentTicket = ticket;
                            displayTicketData();
                        });
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(TrabajadorTicketDetailActivity.this,
                                    "Error al procesar datos del ticket: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(TrabajadorTicketDetailActivity.this,
                                "Error al cargar el ticket: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }
    private void displayTicketData() {
        if (currentTicket == null) return;

        try {
            // Configuración básica de los campos de texto
            tvTicketNumber.setText("Ticket #" + currentTicket.getTicketNumber());
            tvProblemType.setText(currentTicket.getProblemSpinner());
            tvArea.setText(currentTicket.getArea_problema());
            tvDetail.setText(currentTicket.getDetalle());
            tvCreationDate.setText(currentTicket.getCreationDate());
            tvStatus.setText(currentTicket.getStatus());
            tvPriority.setText(currentTicket.getPriority());
            tvComments.setText(currentTicket.getComments());

            // Mejorar la carga de imagen
            String imageString = currentTicket.getImagen();
            if (imageString != null && !imageString.trim().isEmpty()) {
                ivTicketImage.setVisibility(View.VISIBLE);
                try {
                    if (isBase64Image(imageString)) {
                        handleBase64Image(imageString);
                    } else if (imageString.startsWith("content://")) {
                        handleContentUri(imageString);
                    } else {
                        handleUrlImage(imageString);
                    }
                } catch (Exception e) {
                    handleImageError(e);
                }
            } else {
                ivTicketImage.setVisibility(View.GONE);
            }
            // Configurar el spinner y otros controles...
            // [El resto del código se mantiene igual]

        } catch (Exception e) {
            handleGeneralError(e);
        }
    }
    private void handleBase64Image(String base64String) {
        try {
            byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            if (bitmap != null) {
                ivTicketImage.setImageBitmap(bitmap);
            } else {
                throw new IllegalArgumentException("No se pudo decodificar la imagen base64");
            }
        } catch (Exception e) {
            handleImageError(e);
        }
    }
    private void handleContentUri(String contentUri) {
        try {
            // Primero intentamos cargar directamente con Picasso
            Picasso.get()
                    .load(contentUri)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivTicketImage, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("ImageLoading", "Imagen de content Uri cargada exitosamente");
                        }

                        @Override
                        public void onError(Exception e) {
                            // Si falla, intentamos obtener el bitmap directamente
                            try {
                                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                        getContentResolver(),
                                        Uri.parse(contentUri)
                                );
                                if (bitmap != null) {
                                    ivTicketImage.setImageBitmap(bitmap);
                                } else {
                                    handleImageError(new Exception("No se pudo cargar la imagen del content provider"));
                                }
                            } catch (Exception ex) {
                                handleImageError(ex);
                            }
                        }
                    });
        } catch (Exception e) {
            handleImageError(e);
        }
    }
    private void handleUrlImage(String imageUrl) {
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .fit()
                .centerInside()
                .into(ivTicketImage, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d("ImageLoading", "URL de imagen cargada exitosamente");
                    }

                    @Override
                    public void onError(Exception e) {
                        handleImageError(e);
                    }
                });
    }
    private void handleImageError(Exception e) {
        Log.e("ImageLoading", "Error al cargar imagen: " + e.getMessage());
        runOnUiThread(() -> {
            Toast.makeText(this,
                    "Error al cargar la imagen: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            ivTicketImage.setImageResource(R.drawable.error_image);
            ivTicketImage.setVisibility(View.VISIBLE);
        });
    }

    private void handleGeneralError(Exception e) {
        Log.e("DisplayTicketData", "Error: " + e.getMessage(), e);
        Toast.makeText(this,
                "Error al mostrar datos del ticket: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }
    private boolean isBase64Image(String imageString) {
        try {
            return imageString.startsWith("data:image") ||
                    imageString.startsWith("/9j") ||  // JPG
                    imageString.startsWith("iVBORw0KGgo"); // PNG
        } catch (Exception e) {
            return false;
        }
    }
    private void updateTicketStatus() {
        if (currentTicket == null) return;
        String newStatus = spinnerStatus.getSelectedItem().toString();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        // Primero actualizamos Firebase
        currentTicket.updateStatus(newStatus, workerId, mDatabase);
        // Luego actualizamos MockAPI
        updateMockAPI(newStatus, timestamp);
    }
    private void addComment() {
        if (currentTicket == null) return;
        String comment = etNewComment.getText().toString().trim();
        if (comment.isEmpty()) {
            Toast.makeText(this, "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }
        String workerName = getSharedPreferences("UserData", MODE_PRIVATE)
                .getString("nombre", "Trabajador");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        // Actualizar Firebase
        currentTicket.addComment(comment, workerId, workerName, mDatabase);
        // Actualizar MockAPI con el nuevo comentario
        updateMockAPIWithComment(comment, timestamp, workerName);
    }
    private void updateMockAPI(String newStatus, String timestamp) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("status", newStatus);
            jsonBody.put("lastUpdated", timestamp);
            jsonBody.put("assignedWorkerId", workerId);
            jsonBody.put("assignedWorkerName", getSharedPreferences("UserData", MODE_PRIVATE)
                    .getString("nombre", "Trabajador"));

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador/" + currentTicket.getId())
                    .put(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(TrabajadorTicketDetailActivity.this,
                                "Error al actualizar MockAPI: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(TrabajadorTicketDetailActivity.this,
                                    "Estado actualizado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            if (newStatus.equals("Terminado")) {
                                finish(); // Cerrar la actividad si el estado es "Terminado"
                            }
                        });
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error al crear JSON para actualización: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMockAPIWithComment(String comment, String timestamp, String workerName) {
        try {
            // Obtener comentarios existentes o crear nueva lista
            List<String> existingComments = currentTicket.getCommentsList();
            if (existingComments == null) {
                existingComments = new ArrayList<>();
            }

            // Agregar nuevo comentario
            String formattedComment = String.format("[%s] %s: %s",
                    timestamp, workerName, comment);
            existingComments.add(formattedComment);

            // Crear JSON para la actualización
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("comments", new JSONArray(existingComments));
            jsonBody.put("lastUpdated", timestamp);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador/" + currentTicket.getId())
                    .put(body)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(TrabajadorTicketDetailActivity.this,
                                "Error al actualizar comentario en MockAPI: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(TrabajadorTicketDetailActivity.this,
                                    "Comentario agregado correctamente",
                                    Toast.LENGTH_SHORT).show();
                            etNewComment.setText("");

                            // Si el estado actual es "Terminado", cerrar la actividad
                            if (currentTicket.getStatus().equals("Terminado")) {
                                finish();
                            }
                        });
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Error al crear JSON para comentario: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }
}