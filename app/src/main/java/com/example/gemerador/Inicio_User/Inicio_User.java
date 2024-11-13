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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.gemerador.Trabajador.TrabajadorManagement;
import com.example.gemerador.Trabajador.TrabajadorService;
import com.example.gemerador.Trabajador.TrabajadorServiceCallbacks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Inicio_User extends AppCompatActivity {
    private static final String PREFS_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private List<Ticket> tickets;
    private Button btnCrear;
    private ImageView btnCrearTicket, menuIcon, notificationIcon;
    private SharedPreferences sharedPreferences;
    private boolean notificationsEnabled;
    private EditText searchEditText;
    private List<TicketNotification> notifications = new ArrayList<>();
    private TicketNotificationAdapter notificationAdapter;
    private TrabajadorManagement trabajadorManagement;
    private String userRole;
    private static final String ROLE_WORKER = "Trabajador";
    private static final String ROLE_USER = "Usuario";
    private TrabajadorService trabajadorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio_user);
        initializeViews();
        setupSharedPreferences();
        setupRecyclerView();
        checkUserRoleAndSetup();
        setupClickListeners();
        setupSearch();
        initializeNotifications();
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
        tickets = new ArrayList<>();
        adapter = new TicketAdapter(tickets, userRole, new TicketAdapter.TicketActionListener() {
            @Override
            public void onTicketAction(Ticket ticket, String action) {
                handleTicketAction(ticket, action);
            }
        });
        recyclerView.setAdapter(adapter);
        setupRealtimeUpdates();
    }
    private void handleTicketAction(Ticket ticket, String action) {
        switch (action) {
            case "UPDATE_STATUS":
                showUpdateStatusDialog(ticket);
                break;
            case "UPDATE_PRIORITY":
                showUpdatePriorityDialog(ticket);
                break;
            case "ASSIGN_WORKER":
                showAssignWorkerDialog(ticket);
                break;
        }
    }
    private void showUpdateStatusDialog(Ticket ticket) {
        String[] estados = {"Pendiente", "En Progreso", "Terminado", "Cancelado"};

        new AlertDialog.Builder(this)
                .setTitle("Actualizar Estado")
                .setItems(estados, (dialog, which) -> {
                    String newStatus = estados[which];
                    DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                            .getReference("tickets")
                            .child(ticket.getId());

                    ticket.setStatus(newStatus);
                    ticket.setLastUpdated(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()).format(new Date()));

                    ticketRef.setValue(ticket)
                            .addOnSuccessListener(aVoid -> {
                                createTicketUpdateNotification(ticket);
                                adapter.updateTicket(ticket);
                            })
                            .addOnFailureListener(e -> showError("Error al actualizar estado: " + e.getMessage()));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showUpdatePriorityDialog(Ticket ticket) {
        String[] prioridades = {"Baja", "Normal", "Alta", "Urgente"};

        new AlertDialog.Builder(this)
                .setTitle("Actualizar Prioridad")
                .setItems(prioridades, (dialog, which) -> {
                    String newPriority = prioridades[which];
                    DatabaseReference ticketRef = FirebaseDatabase.getInstance()
                            .getReference("tickets")
                            .child(ticket.getId());

                    ticket.setPriority(newPriority);
                    ticket.setLastUpdated(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                            Locale.getDefault()).format(new Date()));

                    ticketRef.setValue(ticket)
                            .addOnSuccessListener(aVoid -> {
                                addNotification(
                                        ticket.getTicketNumber(),
                                        ticket.getStatus(),
                                        "Prioridad actualizada a: " + newPriority,
                                        newPriority
                                );
                                adapter.updateTicket(ticket);
                            })
                            .addOnFailureListener(e -> showError("Error al actualizar prioridad: " + e.getMessage()));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAssignWorkerDialog(Ticket ticket) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Usuarios");

        usersRef.get().addOnSuccessListener(dataSnapshot -> {
            List<String> workerNames = new ArrayList<>();
            List<String> workerIds = new ArrayList<>();

            for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                String role = userSnapshot.child("role").getValue(String.class);
                if ("Trabajador".equals(role)) {
                    String name = userSnapshot.child("nombre").getValue(String.class);
                    if (name != null) {
                        workerNames.add(name);
                        workerIds.add(userSnapshot.getKey());
                    }
                }
            }

            if (workerNames.isEmpty()) {
                showError("No hay trabajadores disponibles");
                return;
            }

            String[] nombres = workerNames.toArray(new String[0]);

            new AlertDialog.Builder(this)
                    .setTitle("Asignar Trabajador")
                    .setItems(nombres, (dialog, which) -> {
                        String workerId = workerIds.get(which);
                        String workerName = nombres[which];

                        ticket.setAssignedWorkerId(workerId);
                        ticket.setAssignedWorkerName(workerName);
                        ticket.setStatus("En Progreso");
                        ticket.setLastUpdated(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss",
                                Locale.getDefault()).format(new Date()));

                        updateTicketInMockAPI(ticket);
                        createTicketUpdateNotification(ticket);
                        adapter.updateTicket(ticket);
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }).addOnFailureListener(e -> showError("Error al cargar trabajadores: " + e.getMessage()));
    }
    private void updateTicketInMockAPI(Ticket ticket) {
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject ticketJson = new JSONObject();
            ticketJson.put("assignedWorkerId", ticket.getAssignedWorkerId());
            ticketJson.put("assignedWorkerName", ticket.getAssignedWorkerName());
            ticketJson.put("status", ticket.getStatus());
            ticketJson.put("lastUpdated", ticket.getLastUpdated());

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(JSON, ticketJson.toString());

            Request request = new Request.Builder()
                    .url("https://66fd14c5c3a184a84d18ff38.mockapi.io/generador/" + ticket.getId())
                    .put(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> showError("Error al actualizar ticket: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() -> showError("Error: " + response.code()));
                    }
                }
            });
        } catch (JSONException e) {
            showError("Error al crear JSON: " + e.getMessage());
        }
    }
    private void checkUserRoleAndSetup() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showError("Usuario no autenticado");
            finish();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(currentUser.getUid());

        userRef.child("role").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                userRole = task.getResult().getValue(String.class);
                setupBasedOnRole();
            } else {
                showError("Error al verificar rol: " + task.getException().getMessage());
            }
        });
    }

    private void setupBasedOnRole() {
        if (ROLE_WORKER.equals(userRole)) {
            setupWorkerView();
        } else {
            setupUserView();
        }
    }

    private void setupWorkerView() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            trabajadorService = new TrabajadorService(currentUser.getUid());
            btnCrear.setVisibility(View.GONE);

            trabajadorService.loadAssignedTickets(new TrabajadorServiceCallbacks.OnTicketsLoadedListener() {
                @Override
                public void onTicketsLoaded(List<Ticket> tickets) {
                    updateTicketList(tickets);
                }
                @Override
                public void onError(String error) {
                    showError(error);
                }
            });
        }
    }

    private void setupUserView() {
        btnCrear.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnCrear.setOnClickListener(v -> startActivity(new Intent(Inicio_User.this, Crear_nuevo_ti.class)));
        notificationIcon.setOnClickListener(v -> showNotificacionesDialog());
        menuIcon.setOnClickListener(v -> startActivity(new Intent(Inicio_User.this, Perfil_user.class)));
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
                runOnUiThread(() -> {
                    Toast.makeText(Inicio_User.this,
                            "Error al cargar los tickets: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(Inicio_User.this,
                                    "Error en la respuesta: " + response.code(),
                                    Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String jsonData = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(jsonData);
                    List<Ticket> newTickets = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        Ticket ticket = new Ticket(
                                jsonObject.optString("ticketNumber", "Sin número"),
                                jsonObject.optString("problemSpinner", "Sin especificar"),
                                jsonObject.optString("area_problema", "Sin área"),
                                jsonObject.optString("detalle", "Sin detalles"),
                                jsonObject.optString("imagen", ""),
                                jsonObject.optString("id", String.valueOf(i)),
                                jsonObject.optString("createdBy", "Usuario no especificado"),
                                jsonObject.optString("creationDate", "Fecha no especificada"),
                                FirebaseAuth.getInstance().getCurrentUser() != null ?
                                        FirebaseAuth.getInstance().getCurrentUser().getUid() : "0"
                        );

                        if (jsonObject.has("status")) {
                            ticket.setStatus(jsonObject.getString("status"));
                        }
                        if (jsonObject.has("priority")) {
                            ticket.setPriority(jsonObject.getString("priority"));
                        }
                        if (jsonObject.has("assignedWorkerId")) {
                            ticket.setAssignedWorkerId(jsonObject.getString("assignedWorkerId"));
                        }
                        if (jsonObject.has("assignedWorkerName")) {
                            ticket.setAssignedWorkerName(jsonObject.getString("assignedWorkerName"));
                        }

                        newTickets.add(ticket);
                    }

                    runOnUiThread(() -> {
                        tickets.clear();
                        tickets.addAll(newTickets);
                        adapter.setTickets(newTickets);

                        if (tickets.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            Toast.makeText(Inicio_User.this,
                                    "Tickets cargados: " + tickets.size(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(Inicio_User.this,
                                    "Error al procesar los datos: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                adapter.searchTickets(s.toString());
            }

        });
    }

    private void updateTicketList(List<Ticket> newTickets) {
        tickets.clear();
        tickets.addAll(newTickets);
        adapter.setTickets(newTickets);

        if (tickets.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
        }
    }

    private void showEmptyState(boolean show) {
        View emptyStateView = findViewById(R.id.emptyStateView);
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    private void setupRealtimeUpdates() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference ticketsRef = FirebaseDatabase.getInstance().getReference("tickets");

        ticketsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Ticket updatedTicket = snapshot.getValue(Ticket.class);
                if (updatedTicket != null) {
                    if (updatedTicket.getUserId().equals(currentUser.getUid()) ||
                            (ROLE_WORKER.equals(userRole) && updatedTicket.isAssignedTo(currentUser.getUid()))) {
                        adapter.updateTicket(updatedTicket);
                        createTicketUpdateNotification(updatedTicket);
                    }
                }
            }

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Error en la actualización en tiempo real: " + error.getMessage());
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        );
    }

    private void updateNotificationIcon() {
        notificationIcon.setImageResource(
                notificationsEnabled ? R.drawable.notifiacion : R.drawable.notifiacion
        );
    }

    private void initializeNotifications() {
        notifications = new ArrayList<>();
        loadSavedNotifications();

        // Debug
        System.out.println("Inicializando notificaciones");

        if (notifications.isEmpty()) {
            System.out.println("Cargando notificación de bienvenida");
            loadSampleNotifications();
        }

        // Debug después de cargar
        System.out.println("Total notificaciones después de inicializar: " + notifications.size());
    }
    private void debugNotifications() {
        System.out.println("Debug de notificaciones:");
        System.out.println("Número total de notificaciones: " + notifications.size());
        for (TicketNotification notification : notifications) {
            System.out.println("Notificación: " + notification.getMessage() +
                    " | Status: " + notification.getStatus() +
                    " | Usuario: " + notification.getUsername());
        }
    }
    private void showNotificacionesDialog() {
        System.out.println("Mostrando diálogo de notificaciones");
        debugNotifications(); // Debug antes de mostrar

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        RecyclerView notificationRecyclerView = dialogView.findViewById(R.id.notificationRecyclerView);

        // Asegurarse de que el RecyclerView existe
        if (notificationRecyclerView == null) {
            System.out.println("Error: notificationRecyclerView es null");
            return;
        }

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<TicketNotification> filteredNotifications = filterNotificationsByUser(notifications);

        System.out.println("Notificaciones filtradas: " + filteredNotifications.size());

        // Crear nuevo adaptador
        notificationAdapter = new TicketNotificationAdapter(filteredNotifications);
        notificationAdapter.setTickets(tickets);

        // Establecer el adaptador
        notificationRecyclerView.setAdapter(notificationAdapter);

        // Crear y mostrar el diálogo
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Notificaciones")
                .setView(dialogView)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Limpiar Todo", (dialogInterface, i) -> {
                    clearAllNotifications();
                })
                .create();

        dialog.show();
    }
    private void addNotification(String ticketId, String status, String message, String priority) {
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

        // Agregar al inicio de la lista
        notifications.add(0, notification);

        // Debug
        System.out.println("Nueva notificación agregada: " + message);
        System.out.println("Total notificaciones: " + notifications.size());

        // Guardar las notificaciones
        saveNotifications();

        // Actualizar el adaptador si existe
        if (notificationAdapter != null) {
            notificationAdapter.notifyDataSetChanged();
        }

        // Actualizar el ícono
        updateNotificationIcon();
    }
    private void createTicketUpdateNotification(Ticket ticket) {
        String message = "";
        String priority = "Normal";

        switch (ticket.getStatus().toLowerCase()) {
            case "en progreso":
                message = "Ticket #" + ticket.getTicketNumber() + " está siendo atendido";
                priority = "Alta";
                break;
            case "terminado":
                message = "Ticket #" + ticket.getTicketNumber() + " ha sido completado";
                priority = "Baja";
                break;
            case "cancelado":
                message = "Ticket #" + ticket.getTicketNumber() + " ha sido cancelado";
                priority = "Alta";
                break;
            default:
                message = "Ticket #" + ticket.getTicketNumber() + " ha sido actualizado";
                break;
        }

        System.out.println("Creando notificación para ticket: " + ticket.getTicketNumber());

        // Siempre crear la notificación, incluso si no hay mensaje específico
        addNotification(
                ticket.getTicketNumber(),
                ticket.getStatus(),
                message,
                priority
        );
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
        if (notificationAdapter != null) {
            notificationAdapter.notifyDataSetChanged();
        }
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
    private void loadSampleNotifications() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());

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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trabajadorService != null) {
            trabajadorService.cleanup();
        }
    }
}