package com.example.gemerador.Trabajador;

import android.os.Handler;
import android.os.Looper;
import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrabajadorService {
    private static final String MOCKAPI_URL = "https://66fd14c5c3a184a84d18ff38.mockapi.io/generador";
    private final String trabajadorId;
    private final OkHttpClient client;
    private final DatabaseReference ticketsRef;
    private Map<String, ValueEventListener> activeListeners;

    public TrabajadorService(String trabajadorId) {
        this.trabajadorId = trabajadorId;
        this.client = new OkHttpClient();
        this.ticketsRef = FirebaseDatabase.getInstance().getReference("tickets");
        this.activeListeners = new HashMap<>();
    }

    public void loadAssignedTickets(final TrabajadorServiceCallbacks.OnTicketsLoadedListener listener) {
        Request request = new Request.Builder()
                .url(MOCKAPI_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        listener.onError("Error al cargar tickets: " + e.getMessage())
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onError("Error en la respuesta: " + response.code())
                    );
                    return;
                }

                if (response.body() == null) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onError("Respuesta vacía del servidor")
                    );
                    return;
                }

                final String responseData = response.body().string();

                try {
                    JSONArray jsonArray = new JSONArray(responseData);
                    List<Ticket> assignedTickets = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonTicket = jsonArray.getJSONObject(i);
                        String assignedWorkerId = jsonTicket.optString("assignedWorkerId", "");

                        if (assignedWorkerId.equals(trabajadorId)) {
                            Ticket ticket = new Ticket(
                                    jsonTicket.getString("ticketNumber"),
                                    jsonTicket.getString("problemSpinner"),
                                    jsonTicket.getString("area_problema"),
                                    jsonTicket.getString("detalle"),
                                    jsonTicket.optString("imagen", ""),
                                    jsonTicket.getString("id"),
                                    jsonTicket.getString("createdBy"),
                                    jsonTicket.getString("creationDate"),
                                    jsonTicket.getString("userId")
                            );
                            ticket.setStatus(jsonTicket.optString("status", "Pendiente"));
                            ticket.setPriority(jsonTicket.optString("priority", "Normal"));
                            ticket.setAssignedWorkerId(assignedWorkerId);
                            ticket.setAssignedWorkerName(jsonTicket.optString("assignedWorkerName", ""));

                            assignedTickets.add(ticket);
                        }
                    }

                    final List<Ticket> finalAssignedTickets = assignedTickets;
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onTicketsLoaded(finalAssignedTickets)
                    );

                } catch (JSONException e) {
                    final String errorMessage = "Error al procesar los tickets: " + e.getMessage() +
                            "\nDatos recibidos: " + responseData;
                    new Handler(Looper.getMainLooper()).post(() ->
                            listener.onError(errorMessage)
                    );
                }
            }
        });
    }

    public void updateTicketStatus(String ticketId, String newStatus, TrabajadorServiceCallbacks.OnOperationCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("lastUpdated", ServerValue.TIMESTAMP);

        // Agregar listener al mapa de listeners activos
        ValueEventListener statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess();
                removeListener(ticketId, "status");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
                removeListener(ticketId, "status");
            }
        };

        addListener(ticketId, "status", statusListener);
        ticketsRef.child(ticketId).updateChildren(updates);
    }

    public void addTicketComment(String ticketId, String comment, TrabajadorServiceCallbacks.OnOperationCompleteListener listener) {
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("text", comment);
        commentData.put("timestamp", ServerValue.TIMESTAMP);
        commentData.put("authorId", trabajadorId);

        // Agregar listener al mapa de listeners activos
        ValueEventListener commentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onSuccess();
                removeListener(ticketId, "comment");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
                removeListener(ticketId, "comment");
            }
        };

        addListener(ticketId, "comment", commentListener);
        ticketsRef.child(ticketId).child("comments").push().setValue(commentData);
    }

    private void addListener(String ticketId, String type, ValueEventListener listener) {
        String key = ticketId + "_" + type;
        activeListeners.put(key, listener);
        ticketsRef.child(ticketId).addValueEventListener(listener);
    }

    private void removeListener(String ticketId, String type) {
        String key = ticketId + "_" + type;
        ValueEventListener listener = activeListeners.remove(key);
        if (listener != null) {
            ticketsRef.child(ticketId).removeEventListener(listener);
        }
    }

    /**
     * Limpia los recursos y listeners cuando el servicio ya no se necesita
     */
    public void cleanup() {
        // Cancelar cualquier llamada pendiente de OkHttp
        if (client != null && client.dispatcher() != null) {
            client.dispatcher().cancelAll();
        }

        // Remover todos los listeners activos
        if (ticketsRef != null && activeListeners != null) {
            for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
                String ticketId = entry.getKey().split("_")[0];
                ticketsRef.child(ticketId).removeEventListener(entry.getValue());
            }
            activeListeners.clear();
        }
    }
}