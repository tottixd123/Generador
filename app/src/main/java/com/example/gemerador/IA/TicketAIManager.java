package com.example.gemerador.IA;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketAIManager {
    private static final String TAG = "TicketAIManager";
    private Context context;
    private TicketPredictor predictor;
    private Handler mainHandler;
    private boolean isInitialized = false;
    private DatabaseReference mDatabase;
    private MockApiService mockApi;

    public TicketAIManager(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.mockApi = RetrofitClient.getClient().create(MockApiService.class);
        initializeComponents();
    }
    private void initializeComponents() {
        try {
            predictor = new TicketPredictor(context);
            isInitialized = true;
            Log.d(TAG, "Sistema de IA inicializado correctamente");
            testMockApiConnection();
            syncTicketsWithFirebase(); // Sincronizar al inicializar
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar componentes", e);
            isInitialized = false;
        }
    }
    public void syncWithMockAPI() {
        try {
            MockApiService service = RetrofitClient.getClient().create(MockApiService.class);
            service.getTickets().enqueue(new Callback<List<Ticket>>() {
                @Override
                public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Ticket> tickets = response.body();
                        Log.d(TAG, "Tickets recuperados de MockAPI: " + tickets.size());
                        for (Ticket ticket : tickets) {
                            if (ticket.getId() != null) {
                                mDatabase.child("tickets").child(ticket.getId())
                                        .setValue(ticket)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Ticket sincronizado: " + ticket.getId()))
                                        .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar ticket: " + ticket.getId(), e));
                            }
                        }
                    } else {
                        String errorMessage;
                        try {
                            errorMessage = response.errorBody() != null ?
                                    response.errorBody().string() : "Unknown error";
                        } catch (IOException e) {
                            errorMessage = "Error al leer respuesta de error: " + e.getMessage();
                            Log.e(TAG, errorMessage, e);
                        }
                        Log.e(TAG, "Error en respuesta de MockAPI: " + errorMessage);
                    }
                }

                @Override
                public void onFailure(Call<List<Ticket>> call, Throwable t) {
                    Log.e(TAG, "Error sincronizando con MockAPI", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar sincronización con MockAPI", e);
        }
    }
    public void testMockApiConnection() {
        mockApi.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Conexión exitosa con MockAPI");
                    if (response.body() != null) {
                        Log.d(TAG, "Tickets encontrados: " + response.body().size());
                    }
                } else {
                    String errorMessage;
                    try {
                        errorMessage = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                    } catch (IOException e) {
                        errorMessage = "Error al leer respuesta de error: " + e.getMessage();
                        Log.e(TAG, errorMessage, e);
                    }
                    Log.e(TAG, "Error en respuesta: " + response.code() + " - " + errorMessage);
                }
            }
            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Error de conexión con MockAPI", t);
            }
        });
    }
    public void syncTicketsWithFirebase() {
        mockApi.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ticket> tickets = response.body();
                    Log.d(TAG, "Tickets recibidos de MockAPI: " + tickets.size());

                    // Crear referencia específica para tickets
                    DatabaseReference ticketsRef = mDatabase.child("tickets");

                    for (Ticket ticket : tickets) {
                        if (ticket.getId() != null) {
                            // Actualizar en Firebase
                            ticketsRef.child(ticket.getId()).setValue(ticket)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Ticket sincronizado con éxito: " + ticket.getId()))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error al sincronizar ticket: " + ticket.getId(), e));

                            // También actualizar estadísticas
                            updateTicketStatsInFirebase(ticket);
                        }
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ?
                                response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Error en respuesta MockAPI: " + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Error al leer respuesta de error", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Error al sincronizar con MockAPI", t);
            }
        });
    }
    private void updateTicketStatsInFirebase(Ticket ticket) {
        String ticketId = ticket.getId();
        if (ticketId == null) return;

        DatabaseReference statsRef = mDatabase.child("ia_stats").child(ticketId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("area_problema", ticket.getArea_problema());
        stats.put("priority", ticket.getPriority());
        stats.put("estimated_time", calculateEstimatedTime(ticket));
        stats.put("assigned_worker", ticket.getAssignedWorkerId());
        stats.put("status", ticket.getStatus());
        stats.put("last_updated", ServerValue.TIMESTAMP);

        statsRef.updateChildren(stats)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Estadísticas actualizadas para ticket: " + ticketId))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error al actualizar estadísticas del ticket: " + ticketId, e));
    }
    public void processNewTicket(Ticket ticket, OnPredictionCompleteListener listener) {
        if (!isInitialized) {
            if (listener != null) {
                listener.onError("El sistema no está inicializado");
            }
            return;
        }

        try {
            TicketMLData mlData = new TicketMLData(
                    ticket.getArea_problema(),
                    ticket.getPriority(),
                    calculateEstimatedTime(ticket),
                    ticket.getAssignedWorkerId(),
                    ticket.getStatus().equals(Ticket.STATUS_COMPLETED)
            );

            float prediction = predictor.predict(mlData);
            Log.d(TAG, "Predicción cruda: " + prediction);

            String recommendedWorker;
            double confidence;

            if (prediction < 0) {
                recommendedWorker = "Sin recomendación";
                confidence = 0;
            } else {
                confidence = Math.min(Math.max(prediction / 100.0, 0), 1);
                int workerIndex = Math.abs((int)(prediction * 100) % 3) + 1;
                recommendedWorker = "Trabajador " + workerIndex;
            }

            Log.d(TAG, "Recomendación: " + recommendedWorker + " (Confianza: " + confidence + ")");

            // Guardar predicción en Firebase
            savePredictionToFirebase(ticket, recommendedWorker, confidence);

            if (listener != null) {
                listener.onPredictionComplete(recommendedWorker, confidence);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar ticket", e);
            if (listener != null) {
                listener.onError("Error en la predicción: " + e.getMessage());
            }
        }
    }

    private void savePredictionToFirebase(Ticket ticket, String recommendedWorker, double confidence) {
        PredictionHistoryItem historyItem = new PredictionHistoryItem(
                ticket.getId(),
                ticket.getArea_problema(),
                ticket.getPriority(),
                recommendedWorker,
                confidence,
                System.currentTimeMillis(),
                false,
                false
        );

        mDatabase.child("prediction_history")
                .push()
                .setValue(historyItem)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error guardando predicción: " + e.getMessage())
                );
    }

    private long calculateEstimatedTime(Ticket ticket) {
        switch (ticket.getPriority()) {
            case Ticket.PRIORITY_HIGH:   return 2 * 3600000;
            case Ticket.PRIORITY_NORMAL: return 4 * 3600000;
            case Ticket.PRIORITY_LOW:    return 8 * 3600000;
            default:                     return 4 * 3600000;
        }
    }

    public void verifyPrediction(String ticketId, String assignedWorkerId, boolean wasSuccessful) {
        mDatabase.child("ia_stats").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Map<String, Object> currentStats = (Map<String, Object>) mutableData.getValue();
                if (currentStats == null) {
                    currentStats = new HashMap<>();
                    currentStats.put("totalPredictions", 0L);
                    currentStats.put("successfulPredictions", 0L);
                    currentStats.put("successRate", 0.0);
                }

                long totalPredictions = (long) currentStats.getOrDefault("totalPredictions", 0L);
                long successfulPredictions = (long) currentStats.getOrDefault("successfulPredictions", 0L);

                if (wasSuccessful) {
                    successfulPredictions++;
                }

                double successRate = totalPredictions > 0 ?
                        ((double) successfulPredictions / totalPredictions) * 100.0 : 0.0;

                currentStats.put("successfulPredictions", successfulPredictions);
                currentStats.put("successRate", successRate);

                mutableData.setValue(currentStats);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot dataSnapshot) {
                if (error != null) {
                    Log.e(TAG, "Error updating prediction verification", error.toException());
                }
            }
        });
    }

    public void forceTraining(OnTrainingCompleteListener listener) {
        mainHandler.postDelayed(() -> {
            if (listener != null) {
                listener.onTrainingComplete();
            }
        }, 5000);
    }

    public boolean isSystemInitialized() {
        return isInitialized;
    }

    public void cleanup() {
        if (predictor != null) {
            predictor.close();
        }
        isInitialized = false;
    }

    public interface OnPredictionCompleteListener {
        void onPredictionComplete(String recommendedWorker, double confidence);
        void onError(String error);
    }

    public interface OnTrainingCompleteListener {
        void onTrainingComplete();
        void onError(String error);
    }
}