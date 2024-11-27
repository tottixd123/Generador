package com.example.gemerador.IA;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketAIManager {
    private static final String TAG = "TicketAIManager";
    private Context context;
    private TicketPredictor predictor;
    private Handler mainHandler;
    private DatabaseReference mDatabase;
    private MockApiService mockApi;
    private boolean isInitialized = false;
    private static final double CONFIDENCE_THRESHOLD = 0.5f;
    private static final double CONFIDENCE_THRESHOLD_LOW = 0.7f;
    private static final double CONFIDENCE_THRESHOLD_HIGH = 0.9f;
    private static final float MAX_PREDICTION = 2.0f;
    private static final float MIN_PREDICTION = 0.0f;

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
            syncTicketsWithFirebase();
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar componentes", e);
            isInitialized = false;
        }
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
                    normalizePriority(ticket.getPriority()),
                    calculateEstimatedTime(ticket),
                    ticket.getAssignedWorkerId(),
                    ticket.getStatus().equals(Ticket.STATUS_COMPLETED)
            );

            float rawPrediction = predictor.predict(mlData);
            Log.d(TAG, "Predicción cruda: " + rawPrediction);

            float confidence = calculateConfidence(rawPrediction);
            Log.d(TAG, "Confianza normalizada: " + confidence);

            String recommendedWorker = getRecommendedWorker(confidence);
            Log.d(TAG, "Trabajador recomendado: " + recommendedWorker);

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
                    .addOnSuccessListener(aVoid -> {
                        updatePredictionStats(false, false, confidence);
                        if (listener != null) {
                            listener.onPredictionComplete(recommendedWorker, confidence);
                        }
                        Log.d(TAG, "Predicción guardada en la base de datos");
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) {
                            listener.onError("Error al guardar predicción: " + e.getMessage());
                        }
                        Log.e(TAG, "Error al guardar predicción", e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar ticket", e);
            if (listener != null) {
                listener.onError("Error en la predicción: " + e.getMessage());
            }
        }
    }
    private String normalizePriority(String priority) {
        if (priority == null) return Ticket.PRIORITY_NORMAL;

        switch (priority.toUpperCase()) {
            case "ALTA":
            case "HIGH":
                return Ticket.PRIORITY_HIGH;
            case "MEDIA":
            case "NORMAL":
                return Ticket.PRIORITY_NORMAL;
            case "BAJA":
            case "LOW":
                return Ticket.PRIORITY_LOW;
            default:
                return Ticket.PRIORITY_NORMAL;
        }
    }
    private String getRecommendedWorker(float confidence) {
        // Asignar trabajador según nivel de confianza
        if (confidence < 0.33f) {
            return "Trabajador 1"; // Casos más simples
        } else if (confidence < 0.66f) {
            return "Trabajador 2"; // Casos de complejidad media
        } else {
            return "Trabajador 3"; // Casos más complejos
        }
    }
    private float calculateConfidence(float rawPrediction) {
        // Ajustar la normalización para ser más estricta
        float normalizedConfidence;

        if (rawPrediction <= MIN_PREDICTION) {
            normalizedConfidence = 0.0f;
        } else if (rawPrediction >= MAX_PREDICTION) {
            normalizedConfidence = 1.0f;
        } else {
            normalizedConfidence = (rawPrediction - MIN_PREDICTION) / (MAX_PREDICTION - MIN_PREDICTION);
        }

        Log.d(TAG, "Predicción raw: " + rawPrediction + ", Confianza normalizada: " + normalizedConfidence);
        return normalizedConfidence;
    }
    private long calculateEstimatedTime(Ticket ticket) {
        switch (ticket.getPriority()) {
            case Ticket.PRIORITY_HIGH:
                return 2 * 3600000; // 2 horas
            case Ticket.PRIORITY_NORMAL:
                return 4 * 3600000; // 4 horas
            case Ticket.PRIORITY_LOW:
                return 8 * 3600000; // 8 horas
            default:
                return 4 * 3600000; // Default 4 horas
        }
    }

    public void verifyPrediction(String ticketId, String actualWorkerId, boolean wasSuccessful) {
        mDatabase.child("prediction_history")
                .orderByChild("ticketId")
                .equalTo(ticketId)
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot predictionSnapshot : snapshot.getChildren()) {
                            PredictionHistoryItem item = predictionSnapshot.getValue(PredictionHistoryItem.class);
                            if (item != null) {
                                updatePredictionVerification(predictionSnapshot.getRef(), wasSuccessful, item.getConfidence());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al verificar predicción", error.toException());
                    }
                });
    }

    private void updatePredictionVerification(DatabaseReference predictionRef, boolean wasSuccessful, double confidence) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("verified", true);
        updates.put("wasSuccessful", wasSuccessful);
        updates.put("verificationTimestamp", ServerValue.TIMESTAMP);

        predictionRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Predicción verificada correctamente");
                    updatePredictionStats(true, wasSuccessful, (float)confidence);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al actualizar verificación", e));
    }
    private void updatePredictionStats(boolean wasVerified, boolean wasSuccessful, float confidence) {
        mDatabase.child("ia_stats").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                Map<String, Object> stats = (Map<String, Object>) mutableData.getValue();
                if (stats == null) {
                    stats = new HashMap<>();
                    stats.put("totalPredictions", 0L);
                    stats.put("successfulPredictions", 0L);
                    stats.put("successRate", 0.0);
                }

                long totalPredictions = ((Number) stats.getOrDefault("totalPredictions", 0L)).longValue();
                long successfulPredictions = ((Number) stats.getOrDefault("successfulPredictions", 0L)).longValue();

                totalPredictions++;

                // Solo contar como exitosa si fue verificada manualmente como exitosa
                if (wasVerified && wasSuccessful) {
                    successfulPredictions++;
                }

                double successRate = (totalPredictions > 0) ?
                        (successfulPredictions * 100.0 / totalPredictions) : 0.0;

                stats.put("totalPredictions", totalPredictions);
                stats.put("successfulPredictions", successfulPredictions);
                stats.put("successRate", successRate);
                stats.put("lastPredictionConfidence", confidence);
                stats.put("lastUpdateTime", ServerValue.TIMESTAMP);

                Log.d(TAG, String.format("Actualización de estadísticas - Total: %d, Exitosas: %d, " +
                                "Tasa: %.2f%%, Confianza: %.2f, Verificada: %b, Fue exitosa: %b",
                        totalPredictions, successfulPredictions, successRate,
                        confidence, wasVerified, wasSuccessful));

                mutableData.setValue(stats);
                return Transaction.success(mutableData);
            }
            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Error actualizando estadísticas", error.toException());
                }
            }
        });
    }
    public void syncWithMockAPI() {
        mockApi.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Ticket ticket : response.body()) {
                        if (ticket.getId() != null) {
                            syncTicketToFirebase(ticket);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Error en sincronización con MockAPI", t);
            }
        });
    }

    private void syncTicketToFirebase(Ticket ticket) {
        mDatabase.child("tickets").child(ticket.getId())
                .setValue(ticket)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ticket sincronizado: " + ticket.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error al sincronizar ticket: " + ticket.getId(), e));
    }

    public void testMockApiConnection() {
        mockApi.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Conexión exitosa con MockAPI");
                } else {
                    Log.e(TAG, "Error en respuesta de MockAPI: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Error de conexión con MockAPI", t);
            }
        });
    }

    private void syncTicketsWithFirebase() {
        mockApi.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Ticket ticket : response.body()) {
                        if (ticket.getId() != null) {
                            syncTicketToFirebase(ticket);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Log.e(TAG, "Error al sincronizar con MockAPI", t);
            }
        });
    }

    public void forceTraining(OnTrainingCompleteListener listener) {
        if (!isInitialized) {
            if (listener != null) {
                listener.onError("Sistema no inicializado");
            }
            return;
        }

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
        void onPredictionComplete(String recommendedWorker, float confidence);
        void onError(String error);
    }
    public interface OnTrainingCompleteListener {
        void onTrainingComplete();
        void onError(String error);
    }
}