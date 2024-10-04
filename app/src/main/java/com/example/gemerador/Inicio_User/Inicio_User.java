package com.example.gemerador.Inicio_User;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private List<Ticket> tickets=new ArrayList<>();
    private Button btnCrear;
    private ImageView btnCrearTicket,menuIcon,notificationIcon;
    private SharedPreferences sharedPreferences;
    private boolean notificationsEnabled;

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

        btnCrear = findViewById(R.id.crear);
        menuIcon = findViewById(R.id.imageView3);
        View imagenCrear = findViewById(R.id.imagencrear);
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
        fetchTicketsFromMockAPI();
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

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Inicio_User.this, "Error al cargar los tickets", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    try {
                        JSONArray jsonArray = new JSONArray(jsonData);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Ticket ticket = new Ticket(
                                    jsonObject.getString("ticketNumber"),
                                    jsonObject.getString("problemSpinner"),
                                    jsonObject.getString("area_problema"),
                                    jsonObject.getString("detalle"),
                                    jsonObject.getString("imagen"),
                                    jsonObject.getString("id")
                            );
                            tickets.add(ticket);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}