package com.example.gemerador.IA;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.R;
import com.example.gemerador.Data_base.Ticket;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketAIActivity extends AppCompatActivity {
    private static final String TAG = "TicketAIActivity";
    private TicketAIManager aiManager;
    private TextView tvTotalPredictions;
    private TextView tvSuccessRate;
    private ProgressBar progressSuccess;
    private RecyclerView rvPredictionHistory;
    private SwitchMaterial switchAutoTraining;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean isAdmin = false;
    private PredictionHistoryAdapter predictionHistoryAdapter;
    private ChildEventListener ticketStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_aiactivity);
        initializeFirebase();
        setupToolbar();
        initializeViews();
        checkAdminPermissions();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gestión de IA");
        }

        // Agregar el manejo de eventos del toolbar
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        tvTotalPredictions = findViewById(R.id.tvTotalPredictions);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        progressSuccess = findViewById(R.id.progressSuccess);
        rvPredictionHistory = findViewById(R.id.rvPredictionHistory);
        switchAutoTraining = findViewById(R.id.switchAutoTraining);
        rvPredictionHistory.setLayoutManager(new LinearLayoutManager(this));
        predictionHistoryAdapter = new PredictionHistoryAdapter(new ArrayList<>());
        rvPredictionHistory.setAdapter(predictionHistoryAdapter);
        setupListeners();
    }
    private void checkAdminPermissions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mDatabase.child("Usuarios").child(currentUser.getUid()).child("role")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String role = snapshot.getValue(String.class);
                            isAdmin = "Administrador".equals(role) || "Admin".equals(role);
                            updateUIBasedOnPermissions();
                            if (isAdmin) {
                                initializeIAStructures();
                                initializeAIManager();
                            } else {
                                showError("Se requieren permisos de administrador para acceder a esta función.");
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error checking admin permissions", error.toException());
                            showError("Error al verificar permisos");
                            finish();
                        }
                    });
        }
    }
    private void initializeIAStructures() {
        // Inicializar ia_stats si no existe
        mDatabase.child("ia_stats").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("totalPredictions", 0);
                    stats.put("successfulPredictions", 0);
                    stats.put("successRate", 0.0);
                    mDatabase.child("ia_stats").setValue(stats)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "ia_stats inicializado correctamente"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error al inicializar ia_stats", e));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al verificar ia_stats", error.toException());
            }
        });

        // Inicializar ia_config si no existe
        mDatabase.child("ia_config").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> config = new HashMap<>();
                    config.put("autoTraining", true);
                    mDatabase.child("ia_config").setValue(config)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "ia_config inicializado correctamente"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error al inicializar ia_config", e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al verificar ia_config", error.toException());
            }
        });
        // Inicializar prediction_history si no existe
        mDatabase.child("prediction_history").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    mDatabase.child("prediction_history").setValue(new ArrayList<>())
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "prediction_history inicializado correctamente"))
                            .addOnFailureListener(e -> Log.e(TAG, "Error al inicializar prediction_history", e));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error al verificar prediction_history", error.toException());
            }
        });
    }
    private void initializeAIManager() {
        aiManager = new TicketAIManager(this);
        if (aiManager.isSystemInitialized()) {
            Log.d(TAG, "Sistema de IA inicializado correctamente");
            aiManager.syncWithMockAPI();
            setupTicketStatusListener(); // Agregar esta línea
            loadInitialData();
        } else {
            showError("Error al inicializar el sistema de IA");
        }
    }
    private void loadInitialData() {
        loadPredictionStats();
        loadWorkerPerformance();
        loadPredictionHistory();
        loadAutoTrainingConfig();
    }

    private void loadAutoTrainingConfig() {
        mDatabase.child("ia_config").child("autoTraining").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean autoTraining = snapshot.getValue(Boolean.class);
                if (autoTraining != null) {
                    switchAutoTraining.setChecked(autoTraining);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading auto training config", error.toException());
            }
        });
    }
    private void loadPredictionStats() {
        mDatabase.child("ia_stats").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> stats = (Map<String, Object>) snapshot.getValue();
                    if (stats != null) {
                        Number totalPredictions = (Number) stats.get("totalPredictions");
                        Number successRate = (Number) stats.get("successRate");

                        if (totalPredictions != null && successRate != null) {
                            updateStatistics(totalPredictions.intValue(), successRate.doubleValue());

                            // Log para debug
                            Log.d(TAG, String.format("Estadísticas cargadas - Total: %d, Tasa: %.2f%%",
                                    totalPredictions.intValue(), successRate.doubleValue()));
                        }
                    }
                } else {
                    // Si no existen estadísticas, inicializarlas
                    Map<String, Object> initialStats = new HashMap<>();
                    initialStats.put("totalPredictions", 0L);
                    initialStats.put("successfulPredictions", 0L);
                    initialStats.put("successRate", 0.0);

                    mDatabase.child("ia_stats").setValue(initialStats)
                            .addOnSuccessListener(aVoid -> updateStatistics(0, 0.0))
                            .addOnFailureListener(e -> Log.e(TAG, "Error initializing stats", e));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading prediction stats", error.toException());
                showError("Error al cargar estadísticas de predicción");
            }
        });
    }
    private void loadWorkerPerformance() {
        mDatabase.child("trabajadores").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<WorkerPerformance> performances = new ArrayList<>();
                AtomicInteger workerCount = new AtomicInteger((int) snapshot.getChildrenCount());

                for (DataSnapshot workerSnapshot : snapshot.getChildren()) {
                    String workerId = workerSnapshot.getKey();
                    String nombre = workerSnapshot.child("nombre").getValue(String.class);

                    WorkerPerformance performance = new WorkerPerformance(workerId, nombre);

                    mDatabase.child("tickets")
                            .orderByChild("assignedWorkerId")
                            .equalTo(workerId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot ticketsSnapshot) {
                                    int completados = 0;
                                    int pendientes = 0;
                                    long tiempoTotal = 0;
                                    int ticketsConTiempo = 0;

                                    for (DataSnapshot ticketSnapshot : ticketsSnapshot.getChildren()) {
                                        String status = ticketSnapshot.child("status").getValue(String.class);
                                        if ("completed".equals(status)) {
                                            completados++;
                                            Long createdAt = ticketSnapshot.child("createdAt").getValue(Long.class);
                                            Long completedAt = ticketSnapshot.child("completedAt").getValue(Long.class);
                                            if (createdAt != null && completedAt != null) {
                                                tiempoTotal += (completedAt - createdAt) / (1000 * 60 * 60.0);
                                                ticketsConTiempo++;
                                            }
                                        } else {
                                            pendientes++;
                                        }
                                    }

                                    performance.setTicketsCompletados(completados);
                                    performance.setTicketsPendientes(pendientes);
                                    if (ticketsConTiempo > 0) {
                                        performance.setTiempoPromedioResolucion(tiempoTotal / ticketsConTiempo);
                                    }

                                    double tasaSatisfaccion = completados + pendientes > 0 ?
                                            (double) completados / (completados + pendientes) : 0.0;
                                    performance.setTasaSatisfaccion(tasaSatisfaccion);

                                    performances.add(performance);

                                    if (workerCount.decrementAndGet() == 0) {
                                        // Ordenar por tasa de satisfacción
                                        Collections.sort(performances, (p1, p2) ->
                                                Double.compare(p2.getTasaSatisfaccion(), p1.getTasaSatisfaccion()));
                                        setupWorkerPerformanceRecyclerView(performances);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e(TAG, "Error loading tickets for worker " + workerId, error.toException());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading worker performance", error.toException());
            }
        });
    }
    private void setupWorkerPerformanceRecyclerView(List<WorkerPerformance> performances) {
        RecyclerView rvWorkerPerformance = findViewById(R.id.rvWorkerPerformance);
        rvWorkerPerformance.setLayoutManager(new LinearLayoutManager(this));
        WorkerPerformanceAdapter adapter = new WorkerPerformanceAdapter(performances);
        rvWorkerPerformance.setAdapter(adapter);
    }
    private void loadPredictionHistory() {
        mDatabase.child("prediction_history").limitToLast(10).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<PredictionHistoryItem> historyItems = new ArrayList<>();
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    PredictionHistoryItem item = childSnapshot.getValue(PredictionHistoryItem.class);
                    if (item != null) {
                        historyItems.add(item);
                    }
                }
                predictionHistoryAdapter.updateData(historyItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error loading prediction history", error.toException());
                showError("Error al cargar historial de predicciones");
            }
        });
    }

    private void setupListeners() {
        FloatingActionButton fabNewPrediction = findViewById(R.id.fabNewPrediction);
        fabNewPrediction.setOnClickListener(v -> showNewPredictionDialog());

        switchAutoTraining.setOnCheckedChangeListener((buttonView, isChecked) ->
                updateAutoTrainingConfig(isChecked));

        findViewById(R.id.btnForceTraining).setOnClickListener(v -> forceModelTraining());
    }

    private void showNewPredictionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Nueva Predicción")
                .setMessage("¿Desea realizar una predicción con los datos actuales?")
                .setPositiveButton("Realizar Predicción", (dialog, which) -> runTestPrediction())
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void runTestPrediction() {
        if (aiManager != null && aiManager.isSystemInitialized()) {
            Ticket testTicket = new Ticket();
            testTicket.setArea_problema("Mantenimiento de impresora");
            testTicket.setPriority(Ticket.PRIORITY_HIGH);
            testTicket.setStatus(Ticket.STATUS_PENDING);

            aiManager.processNewTicket(testTicket, new TicketAIManager.OnPredictionCompleteListener() {
                @Override
                public void onPredictionComplete(String recommendedWorker, double confidence) {
                    runOnUiThread(() -> {
                        String message = String.format(
                                "Predicción exitosa\nÁrea: %s\nPrioridad: %s\nTrabajador recomendado: %s\nConfianza: %.1f%%",
                                testTicket.getArea_problema(),
                                testTicket.getPriority(),
                                recommendedWorker,
                                confidence * 100
                        );
                        showSuccess(message);
                        savePredictionToHistory(testTicket, recommendedWorker, confidence);
                        updatePredictionStatsWithConfidence(confidence);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError("Error en la predicción: " + error));
                }
            });
        } else {
            showError("El sistema de IA no está inicializado correctamente");
        }
    }
    private void updatePredictionStats(boolean wasSuccessful) {
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

                // Obtener valores actuales
                long totalPredictions = ((Number) stats.get("totalPredictions")).longValue();
                long successfulPredictions = ((Number) stats.get("successfulPredictions")).longValue();

                // Incrementar predicciones exitosas si fue exitosa
                if (wasSuccessful) {
                    successfulPredictions++;
                }

                // Calcular nueva tasa de éxito
                double successRate = (totalPredictions > 0) ?
                        ((double) successfulPredictions / totalPredictions) * 100.0 : 0.0;

                // Actualizar valores
                stats.put("totalPredictions", totalPredictions);
                stats.put("successfulPredictions", successfulPredictions);
                stats.put("successRate", successRate);

                Log.d(TAG, String.format("Verificación de predicción - Total: %d, Exitosas: %d, Tasa: %.2f%%, Fue exitosa: %b",
                        totalPredictions, successfulPredictions, successRate, wasSuccessful));

                mutableData.setValue(stats);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Error al actualizar estadísticas", error.toException());
                    return;
                }
                if (committed && snapshot.exists()) {
                    Map<String, Object> stats = (Map<String, Object>) snapshot.getValue();
                    if (stats != null) {
                        runOnUiThread(() -> {
                            long total = ((Number) stats.get("totalPredictions")).longValue();
                            double rate = ((Number) stats.get("successRate")).doubleValue();
                            updateStatistics((int) total, rate);
                        });
                    }
                }
            }
        });
    }
    private void savePredictionToHistory(Ticket ticket, String recommendedWorker, double confidence) {
        DatabaseReference historyRef = mDatabase.child("prediction_history");

        // Crear el objeto de predicción
        PredictionHistoryItem historyItem = new PredictionHistoryItem();
        historyItem.setTicketId(ticket.getId());
        historyItem.setAreaProblema(ticket.getArea_problema());
        historyItem.setPriority(ticket.getPriority());
        historyItem.setRecommendedWorker(recommendedWorker);
        historyItem.setConfidence(confidence);
        historyItem.setTimestamp(System.currentTimeMillis());
        historyItem.setVerified(false);
        historyItem.setWasSuccessful(false);

        // Guardar la predicción
        historyRef.push().setValue(historyItem)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Predicción guardada exitosamente");
                    loadPredictionHistory();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error al guardar predicción", e));
    }
    private void updateAutoTrainingConfig(boolean enabled) {
        mDatabase.child("ia_config").child("autoTraining").setValue(enabled)
                .addOnSuccessListener(aVoid ->
                        showSuccess("Entrenamiento automático " + (enabled ? "activado" : "desactivado")))
                .addOnFailureListener(e -> {
                    showError("Error al actualizar configuración");
                    switchAutoTraining.setChecked(!enabled);
                });
    }

    private void forceModelTraining() {
        if (aiManager != null && aiManager.isSystemInitialized()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Entrenamiento Forzado")
                    .setMessage("¿Está seguro de que desea forzar el entrenamiento del modelo? Esto puede tomar varios minutos.")
                    .setPositiveButton("Entrenar", (dialog, which) -> {
                        showSuccess("Iniciando entrenamiento forzado...");
                        aiManager.forceTraining(new TicketAIManager.OnTrainingCompleteListener() {
                            @Override
                            public void onTrainingComplete() {
                                runOnUiThread(() -> showSuccess("Entrenamiento completado"));
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> showError("Error en el entrenamiento: " + error));
                            }
                        });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        } else {
            showError("El sistema de IA no está inicializado correctamente");
        }
    }

    private void updateUIBasedOnPermissions() {
        FloatingActionButton fab = findViewById(R.id.fabNewPrediction);
        fab.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        switchAutoTraining.setEnabled(isAdmin);
        findViewById(R.id.btnForceTraining).setEnabled(isAdmin);
    }
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private void setupTicketStatusListener() {
        if (ticketStatusListener != null) {
            mDatabase.child("tickets").removeEventListener(ticketStatusListener);
        }
        ticketStatusListener = new ChildEventListener() {
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {
                String status = snapshot.child("status").getValue(String.class);
                String ticketId = snapshot.getKey();
                String workerId = snapshot.child("assignedWorkerId").getValue(String.class);

                if ("completed".equals(status) && ticketId != null && workerId != null) {
                    verifyPrediction(ticketId, workerId);
                }
            }
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error en listener de tickets", error.toException());
            }
        };
        mDatabase.child("tickets").addChildEventListener(ticketStatusListener);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiManager != null) {
            aiManager.cleanup();
        }
    }
    private void verifyPrediction(String ticketId, String actualWorkerId) {
        // Primero buscar en el historial de predicciones recientes
        mDatabase.child("prediction_history")
                .orderByChild("ticketId")
                .equalTo(ticketId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean predictionFound = false;

                        for (DataSnapshot predictionSnapshot : snapshot.getChildren()) {
                            PredictionHistoryItem prediction = predictionSnapshot.getValue(PredictionHistoryItem.class);
                            if (prediction != null && !prediction.isVerified()) {
                                predictionFound = true;
                                boolean wasSuccessful = prediction.getRecommendedWorker().equals(actualWorkerId);

                                // Actualizar la predicción en la base de datos
                                DatabaseReference predictionRef = predictionSnapshot.getRef();
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("verified", true);
                                updates.put("wasSuccessful", wasSuccessful);
                                updates.put("verificationTimestamp", System.currentTimeMillis());

                                predictionRef.updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            // Actualizar estadísticas globales
                                            updatePredictionStats(wasSuccessful);
                                            // Notificar al sistema de IA
                                            if (aiManager != null) {
                                                aiManager.verifyPrediction(ticketId, actualWorkerId, wasSuccessful);
                                            }
                                            Log.d(TAG, String.format("Predicción verificada - ID: %s, Exitosa: %b",
                                                    ticketId, wasSuccessful));
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Error al actualizar predicción", e));
                            }
                        }

                        // Si no se encontró la predicción en el historial reciente
                        if (!predictionFound) {
                            Log.w(TAG, "No se encontró predicción para el ticket: " + ticketId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al verificar predicción", error.toException());
                    }
                });
    }
    private void resetStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPredictions", 0L);
        stats.put("successfulPredictions", 0L);
        stats.put("successRate", 0.0);

        mDatabase.child("ia_stats").setValue(stats)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Estadísticas reiniciadas"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al reiniciar estadísticas", e));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ticket_ai, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_reset_stats) {
            if (isAdmin) {
                showResetStatsDialog();
            } else {
                showError("Se requieren permisos de administrador");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void showResetStatsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reiniciar Estadísticas")
                .setMessage("¿Está seguro de que desea reiniciar todas las estadísticas? Esta acción no se puede deshacer.")
                .setPositiveButton("Reiniciar", (dialog, which) -> {
                    resetStats();
                    showSuccess("Estadísticas reiniciadas");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void updatePredictionStatsWithConfidence(double confidence) {
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

                long totalPredictions = ((Number) stats.get("totalPredictions")).longValue();
                long successfulPredictions = ((Number) stats.get("successfulPredictions")).longValue();

                totalPredictions++;
                if (confidence >= 0.5) {
                    successfulPredictions++;
                }

                double successRate = totalPredictions > 0 ?
                        (successfulPredictions * 100.0 / totalPredictions) : 0.0;

                stats.put("totalPredictions", totalPredictions);
                stats.put("successfulPredictions", successfulPredictions);
                stats.put("successRate", successRate);

                mutableData.setValue(stats);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Error al actualizar estadísticas", error.toException());
                    return;
                }
                if (committed && snapshot.exists()) {
                    Map<String, Object> stats = (Map<String, Object>) snapshot.getValue();
                    if (stats != null) {
                        runOnUiThread(() -> {
                            long total = ((Number) stats.get("totalPredictions")).longValue();
                            double rate = ((Number) stats.get("successRate")).doubleValue();
                            updateStatistics((int) total, rate);
                        });
                    }
                }
            }
        });
    }
    private void updateStatistics(int totalPredictions, double successRate) {
        runOnUiThread(() -> {
            if (tvTotalPredictions != null) {
                tvTotalPredictions.setText(String.format(Locale.getDefault(),
                        "Total de predicciones: %d", totalPredictions));
            }
            if (tvSuccessRate != null) {
                // Asegurar que la tasa tenga al menos un decimal
                tvSuccessRate.setText(String.format(Locale.getDefault(),
                        "Tasa de éxito: %.1f%%", Math.max(0.0, successRate)));
            }
            if (progressSuccess != null) {
                // Asegurar que el progreso esté entre 0 y 100
                progressSuccess.setProgress((int) Math.min(100, Math.max(0, successRate)));
            }

            Log.d(TAG, String.format(Locale.getDefault(),
                    "Estadísticas actualizadas - Total: %d, Tasa: %.1f%%",
                    totalPredictions, successRate));
        });
    }
}