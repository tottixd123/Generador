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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.TicketAdapter;
import com.example.gemerador.Crear_Ti.Crear_nuevo_ti;
import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.Perfil.Perfil_user;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Inicio_User extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TicketAdapter adapter;
    private List<Ticket> tickets = new ArrayList<>();
    private Button btnCrear;
    private ImageView btnCrearTicket, menuIcon, notificationIcon;
    private SharedPreferences sharedPreferences;
    private boolean notificationsEnabled;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inicio_user);
        sharedPreferences = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TicketAdapter(tickets);
        recyclerView.setAdapter(adapter);
        searchEditText = findViewById(R.id.editTextText2);
        btnCrear = findViewById(R.id.crear);
        menuIcon = findViewById(R.id.imageView3);
        notificationIcon = findViewById(R.id.imageView4);
        updateNotificationIcon();

        btnCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Inicio_User.this, Crear_nuevo_ti.class);
                startActivity(intent);
            }
        });
        notificationIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNotifications();
            }
        });
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Inicio_User.this, Perfil_user.class);
                startActivity(intent);
            }
        });
        // Configurar búsqueda
        setupSearch();

        // Filtrar por usuario actual
        filterTicketsByCurrentUser();
        fetchTicketsFromMockAPI();
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

    private void toggleNotifications() {
        notificationsEnabled = !notificationsEnabled;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("notificationsEnabled", notificationsEnabled);
        editor.apply();

        updateNotificationIcon();

        String message = notificationsEnabled ? "Notificaciones activadas" : "Notificaciones desactivadas";
        Toast.makeText(Inicio_User.this, message, Toast.LENGTH_SHORT).show();
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