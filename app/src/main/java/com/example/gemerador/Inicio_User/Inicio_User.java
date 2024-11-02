package com.example.gemerador.Inicio_User;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Crear_Ti.Crear_nuevo_ti;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.Notificaciones.TicketNotification;
import com.example.gemerador.Notificaciones.TicketNotificationAdapter;
import com.example.gemerador.Perfil.Perfil_user;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Inicio_User extends AppCompatActivity {
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private List<Ticket> tickets = new ArrayList<>();
    private Button btnCrear;
    private ImageView btnCrearTicket, menuIcon, notificationIcon;
    private SharedPreferences sharedPreferences;
    private boolean notificationsEnabled;
    private EditText searchEditText;
    private List<TicketNotification> notifications = new ArrayList<>();
    private TicketNotificationAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio_user);
        initializeViews();
        setupSharedPreferences();
        setupRecyclerView();
        setupClickListeners();
        setupSearch();
        initializeNotifications();
        filterTicketsByCurrentUser();
        fetchTicketsFromMockAPI();
    }
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchEditText = findViewById(R.id.editTextText2);
        btnCrear = findViewById(R.id.crear);
        menuIcon = findViewById(R.id.imageView3);
        notificationIcon = findViewById(R.id.imageView4);
    }
    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);
        updateNotificationIcon();
    }
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketAdapter(tickets);
        recyclerView.setAdapter(adapter);
    }
    private void setupClickListeners() {
        btnCrear.setOnClickListener(v -> startActivity(new Intent(Inicio_User.this, Crear_nuevo_ti.class)));

        notificationIcon.setOnClickListener(v -> showNotificacionesDialog());

        menuIcon.setOnClickListener(v -> startActivity(new Intent(Inicio_User.this, Perfil_user.class)));
    }
    private void initializeNotifications() {
        notifications = new ArrayList<>();
        loadSavedNotifications(); // Load saved notifications first
        if (notifications.isEmpty()) {
            loadSampleNotifications();
        }

        // Add observer for new tickets
        adapter.setOnTicketAddedListener(new TicketAdapter.OnTicketAddedListener() {
            @Override
            public void onTicketAdded(Ticket ticket) {
                // Create notification for new ticket
                addNotification(
                        ticket.getTicketNumber(),
                        "Pendiente",
                        "Nuevo ticket creado: " + ticket.getProblemSpinner(),
                        "Alta"
                );
            }
        });
    }

    private void showNotificacionesDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        RecyclerView notificationRecyclerView = dialogView.findViewById(R.id.notificationRecyclerView);

        loadSavedNotifications();

        setupNotificationRecyclerView(notificationRecyclerView);

        new AlertDialog.Builder(this)
                .setTitle("Notificaciones")
                .setView(dialogView)
                .setPositiveButton("Cerrar", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Limpiar Todo", (dialog, which) -> {
                    clearAllNotifications();
                    dialog.dismiss();
                })
                .show();
    }
    private void setupNotificationRecyclerView(RecyclerView recyclerView) {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<TicketNotification> filteredNotifications = filterNotificationsByUser(notifications);
        notificationAdapter = new TicketNotificationAdapter(filteredNotifications);
        notificationAdapter.setTickets(tickets);
        recyclerView.setAdapter(notificationAdapter);
    }
    private void clearAllNotifications() {
        notifications.clear();
        notificationAdapter.notifyDataSetChanged();
        saveNotifications();
    }
    private void saveNotifications() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (TicketNotification notification : notifications) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("ticketId", notification.getTicketId());
                jsonObject.put("status", notification.getStatus());
                jsonObject.put("message", notification.getMessage());
                jsonObject.put("timestamp", notification.getTimestamp());
                jsonObject.put("priority", notification.getPriority());
                jsonObject.put("username", notification.getUsername());
                jsonArray.put(jsonObject);
            }
            sharedPreferences.edit()
                    .putString(KEY_NOTIFICATIONS, jsonArray.toString())
                    .apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void loadSavedNotifications() {
        String savedNotifications = sharedPreferences.getString(KEY_NOTIFICATIONS, "");
        if (!savedNotifications.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(savedNotifications);
                notifications.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    notifications.add(new TicketNotification(
                            jsonObject.getString("ticketId"),
                            jsonObject.getString("status"),
                            jsonObject.getString("message"),
                            jsonObject.getString("timestamp"),
                            jsonObject.getString("priority"),
                            jsonObject.getString("username")
                    ));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                loadSampleNotifications();
            }
        } else {
            loadSampleNotifications();
        }
    }
    // Método para agregar una nueva notificación
    public void addNotification(String ticketId, String status, String message, String priority) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        TicketNotification notification = new TicketNotification(
                ticketId,
                status,
                message,
                currentTime,
                priority,
                getCurrentUsername()
        );

        notifications.add(0, notification);
        saveNotifications();

        if (notificationAdapter != null) {
            notificationAdapter.notifyItemInserted(0);
        }

        // Show notification badge or update UI as needed
        updateNotificationIcon();
    }
    private List<TicketNotification> filterNotificationsByUser(List<TicketNotification> allNotifications) {
        String currentUsername = getCurrentUsername();
        List<TicketNotification> filtered = new ArrayList<>();

        for (TicketNotification notification : allNotifications) {
            if (notification.getUsername().equals(currentUsername) ||
                    notification.getUsername().equals("Sistema")) {
                filtered.add(notification);
            }
        }

        return filtered;
    }
    private void loadSampleNotifications() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

        // Agregar notificación de bienvenida del sistema
        notifications.add(new TicketNotification(
                "Sistema",
                "Información",
                "Bienvenido al sistema de tickets",
                currentTime,
                "Información",
                "Sistema"
        ));
    }
    private String getCurrentUsername() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ?
                (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail()) :
                "Usuario";
    }
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                adapter.searchTickets(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void filterTicketsByCurrentUser() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            adapter.filterByUser(currentUser.getUid());
        }
    }

    private void updateNotificationIcon() {
        if (notificationsEnabled) {
            notificationIcon.setImageResource(R.drawable.notifiacion); // Make sure you have this drawable
        } else {
            notificationIcon.setImageResource(R.drawable.notifiacion); // Make sure you have this drawable
        }
    }

    private void fetchTicketsFromMockAPI() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador")
                .build();

        System.out.println("Iniciando fetch de tickets...");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println("Error en la llamada: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(Inicio_User.this,
                                "Error al cargar los tickets: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("Respuesta no exitosa: " + response.code());
                    runOnUiThread(() ->
                            Toast.makeText(Inicio_User.this,
                                    "Error en la respuesta: " + response.code(),
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String jsonData = response.body().string();
                System.out.println("Respuesta recibida: " + jsonData);

                try {
                    JSONArray jsonArray = new JSONArray(jsonData);
                    System.out.println("Número de tickets en JSON: " + jsonArray.length());

                    List<Ticket> newTickets = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        // Obtener valores con valores por defecto si no existen
                        String ticketNumber = jsonObject.optString("ticketNumber", "Sin número");
                        String problemSpinner = jsonObject.optString("problemSpinner", "Sin especificar");
                        String areaProblema = jsonObject.optString("area_problema", "Sin área");
                        String detalle = jsonObject.optString("detalle", "Sin detalles");
                        String imagen = jsonObject.optString("imagen", "");
                        String id = jsonObject.optString("id", "0");
                        String createdBy = jsonObject.optString("createdBy", "Usuario no especificado");
                        String creationDate = jsonObject.optString("creationDate", "Fecha no especificada");
                        String userId = jsonObject.optString("userId", FirebaseAuth.getInstance().getCurrentUser() != null ?
                                FirebaseAuth.getInstance().getCurrentUser().getUid() : "0");

                        Ticket ticket = new Ticket(
                                ticketNumber,
                                problemSpinner,
                                areaProblema,
                                detalle,
                                imagen,
                                id,
                                createdBy,
                                creationDate,
                                userId
                        );
                        newTickets.add(ticket);
                        System.out.println("Ticket agregado: " + ticket.getTicketNumber());
                    }

                    runOnUiThread(() -> {
                        tickets.clear();
                        tickets.addAll(newTickets);
                        adapter.notifyDataSetChanged();
                        filterTicketsByCurrentUser(); // Aplicar el filtro después de cargar los tickets

                        for (Ticket ticket : newTickets) {
                            addNotification(
                                    ticket.getTicketNumber(),
                                    "Nuevo",
                                    "Ticket importado: " + ticket.getProblemSpinner(),
                                    "Información"
                            );
                        }

                        if (tickets.isEmpty()) {
                            Toast.makeText(Inicio_User.this,
                                    "No hay tickets disponibles",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(Inicio_User.this,
                                    "Tickets cargados: " + tickets.size(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    System.out.println("Error parseando JSON: " + e.getMessage());
                    runOnUiThread(() ->
                            Toast.makeText(Inicio_User.this,
                                    "Error al procesar los datos: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}