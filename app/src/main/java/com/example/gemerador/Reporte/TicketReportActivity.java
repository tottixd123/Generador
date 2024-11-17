package com.example.gemerador.Reporte;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TicketReportActivity extends AppCompatActivity {
    private static final String TAG = "TicketReport";
    private static final String MOCKAPI_URL = "https://66fd14c5c3a184a84d18ff38.mockapi.io/generador";
    private TextView tvTotalTickets, tvResolvedTickets, tvPendingTickets;
    private TextView tvMonthlyTickets, tvMonthlyResolved;
    private CardView cardViewMonthly, cardViewTotal;
    private ProgressBar progressBar;
    private FirebaseUser currentUser;
    private String userRole;
    private LinearLayout layoutCommonIncidents, layoutTimeAnalysis, layoutWorkerPerformance;
    private TextView tvTodayTickets, tvDailyResolutionRate;
    private Map<String, Integer> commonIncidents = new HashMap<>();
    private Map<String, Long> averageResolutionTimes = new HashMap<>();
    private static class WorkerStats {
        public int totalTickets;
        public int resolvedTickets;
        public long averageResolutionTime;

        public WorkerStats() {
            totalTickets = 0;
            resolvedTickets = 0;
            averageResolutionTime = 0;
        }
    }
    private Map<String, WorkerStats> workerStats = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_report);
        workerStats = new HashMap<>();

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        initializeViews();
        setupToolbar();
        checkAdminAndLoadData();
    }
    private void checkAdminAndLoadData() {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("Usuarios")
                .child(currentUser.getUid());

        userRef.child("role").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                userRole = task.getResult().getValue(String.class);
                if ("Administrador".equals(userRole) || "Admin".equals(userRole)) {
                    loadTicketStatistics();
                } else {
                    Toast.makeText(this, "Acceso no autorizado", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Toast.makeText(this, "Error al verificar permisos", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    private void initializeViews() {
        tvTotalTickets = findViewById(R.id.tvTotalTickets);
        tvResolvedTickets = findViewById(R.id.tvResolvedTickets);
        tvPendingTickets = findViewById(R.id.tvPendingTickets);
        tvMonthlyTickets = findViewById(R.id.tvMonthlyTickets);
        tvMonthlyResolved = findViewById(R.id.tvMonthlyResolved);
        cardViewMonthly = findViewById(R.id.cardViewMonthly);
        cardViewTotal = findViewById(R.id.cardViewTotal);
        progressBar = findViewById(R.id.progressBar);
        layoutCommonIncidents = findViewById(R.id.layoutCommonIncidents);
        layoutTimeAnalysis = findViewById(R.id.layoutTimeAnalysis);
        layoutWorkerPerformance = findViewById(R.id.layoutWorkerPerformance);
        tvTodayTickets = findViewById(R.id.tvTodayTickets);
        tvDailyResolutionRate = findViewById(R.id.tvDailyResolutionRate);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Reportes de Tickets");
        }
    }
    private void loadTicketStatistics() {
        showLoading(true);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(MOCKAPI_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(TicketReportActivity.this,
                            "Error de conexión: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String jsonData = response.body().string();
                    JSONArray tickets = new JSONArray(jsonData);
                    processTicketData(tickets);

                } catch (Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(TicketReportActivity.this,
                                "Error al procesar datos: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
                }
            }
        });
    }
    private void processTicketData(JSONArray ticketArray) {
        try {
            commonIncidents.clear();
            averageResolutionTimes.clear();
            workerStats.clear();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date startOfDay = calendar.getTime();

            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);

            int totalTickets = ticketArray.length();
            int resolvedTickets = 0;
            int monthlyTickets = 0;
            int monthlyResolved = 0;
            int todayTickets = 0;
            int todayResolved = 0;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            for (int i = 0; i < ticketArray.length(); i++) {
                JSONObject ticket = ticketArray.getJSONObject(i);
                String status = ticket.optString("status", "").toLowerCase().trim();
                String problem = ticket.optString("problemSpinner", "Sin categoría");
                String workerId = ticket.optString("assignedWorkerId", "");
                String workerName = ticket.optString("assignedWorkerName", "Sin asignar");
                String creationDateStr = ticket.optString("creationDate", "");
                String lastUpdatedStr = ticket.optString("lastUpdated", creationDateStr);
                boolean isResolved = status.equals("resuelto") || status.equals("terminado") ||
                        status.equals("completed") || status.equals("resolved");
                commonIncidents.put(problem, commonIncidents.getOrDefault(problem, 0) + 1);
                if (!creationDateStr.isEmpty()) {
                    try {
                        Date creationDate = sdf.parse(creationDateStr);
                        if (creationDate != null) {
                            if (creationDate.after(startOfDay)) {
                                todayTickets++;
                                if (isResolved) todayResolved++;
                            }
                            calendar.setTime(creationDate);
                            if (calendar.get(Calendar.MONTH) == currentMonth &&
                                    calendar.get(Calendar.YEAR) == currentYear) {
                                monthlyTickets++;
                                if (isResolved) monthlyResolved++;
                            }
                            if (isResolved && !lastUpdatedStr.isEmpty()) {
                                Date resolutionDate = sdf.parse(lastUpdatedStr);
                                if (resolutionDate != null) {
                                    long resolutionTime = resolutionDate.getTime() - creationDate.getTime();
                                    averageResolutionTimes.put(problem,
                                            averageResolutionTimes.getOrDefault(problem, 0L) + resolutionTime);
                                    if (!workerId.isEmpty()) {
                                        WorkerStats stats = workerStats.computeIfAbsent(workerName,
                                                k -> new WorkerStats());
                                        stats.totalTickets++;
                                        stats.resolvedTickets++;
                                        stats.averageResolutionTime =
                                                (stats.averageResolutionTime * (stats.resolvedTickets - 1) +
                                                        resolutionTime) / stats.resolvedTickets;
                                    }
                                }
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date: " + e.getMessage());
                    }
                }
                if (isResolved) {
                    resolvedTickets++;
                }
                if (!workerId.isEmpty()) {
                    WorkerStats stats = workerStats.computeIfAbsent(workerName, k -> new WorkerStats());
                    stats.totalTickets++;
                }
            }
            final int finalResolvedTickets = resolvedTickets;
            final int finalMonthlyTickets = monthlyTickets;
            final int finalMonthlyResolved = monthlyResolved;
            final int finalTodayTickets = todayTickets;
            final int finalTodayResolved = todayResolved;
            runOnUiThread(() -> {
                updateUI(totalTickets, finalResolvedTickets, finalMonthlyTickets, finalMonthlyResolved);
                updateDailyStats(finalTodayTickets, finalTodayResolved);
                updateCommonIncidents();
                updateTimeAnalysis();
                updateWorkerPerformance();
                showLoading(false);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error processing ticket data: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "Error al procesar tickets: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            });
        }
    }
    private void updateDailyStats(int todayTickets, int todayResolved) {
        tvTodayTickets.setText(String.valueOf(todayTickets));
        float resolutionRate = todayTickets > 0 ? (todayResolved * 100f) / todayTickets : 0;
        tvDailyResolutionRate.setText(String.format(Locale.getDefault(),
                "Resueltos hoy: %d (%.1f%%)", todayResolved, resolutionRate));
    }
    private void updateCommonIncidents() {
        layoutCommonIncidents.removeAllViews();
        List<Map.Entry<String, Integer>> sortedIncidents = new ArrayList<>(commonIncidents.entrySet());
        sortedIncidents.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, Integer> entry : sortedIncidents) {
            TextView textView = new TextView(this);
            textView.setText(String.format(Locale.getDefault(),
                    "%s: %d tickets", entry.getKey(), entry.getValue()));
            textView.setPadding(0, 4, 0, 4);
            layoutCommonIncidents.addView(textView);
        }
    }
    private void updateTimeAnalysis() {
        layoutTimeAnalysis.removeAllViews();
        List<Map.Entry<String, Long>> sortedTimes = new ArrayList<>(averageResolutionTimes.entrySet());
        sortedTimes.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, Long> entry : sortedTimes) {
            TextView textView = new TextView(this);
            long averageHours = entry.getValue() / (1000 * 60 * 60); // Convertido a horas
            textView.setText(String.format(Locale.getDefault(),
                    "%s: %d horas promedio", entry.getKey(), averageHours));
            textView.setPadding(0, 4, 0, 4);
            layoutTimeAnalysis.addView(textView);
        }
    }
    private void updateWorkerPerformance() {
        layoutWorkerPerformance.removeAllViews();
        List<Map.Entry<String, WorkerStats>> sortedWorkers =
                new ArrayList<>(workerStats.entrySet());
        sortedWorkers.sort((a, b) ->
                Integer.compare(b.getValue().totalTickets, a.getValue().totalTickets));
        for (Map.Entry<String, WorkerStats> entry : sortedWorkers) {
            TextView textView = new TextView(this);
            WorkerStats stats = entry.getValue();
            float resolutionRate = stats.totalTickets > 0 ?
                    (stats.resolvedTickets * 100f) / stats.totalTickets : 0;
            float avgHours = stats.resolvedTickets > 0 ?
                    stats.averageResolutionTime / (1000f * 60 * 60) : 0;
            String performanceText = String.format(Locale.getDefault(),
                    "%s:\nTickets Totales: %d\nResueltos: %d (%.1f%%)\nTiempo Promedio: %.1f horas",
                    entry.getKey(),
                    stats.totalTickets,
                    stats.resolvedTickets,
                    resolutionRate,
                    avgHours
            );
            textView.setText(performanceText);
            textView.setPadding(0, 8, 0, 8);
            layoutWorkerPerformance.addView(textView);
        }
    }
    private void updateUI(int total, int resolved, int monthly, int monthlyResolved) {
        resolved = Math.min(resolved, total);
        monthlyResolved = Math.min(monthlyResolved, monthly);
        int pending = total - resolved;
        tvTotalTickets.setText(String.valueOf(total));
        float resolvedPercentage = total > 0 ? (resolved * 100f) / total : 0;
        float monthlyResolvedPercentage = monthly > 0 ? (monthlyResolved * 100f) / monthly : 0;
        String resolvedText = String.format(Locale.getDefault(),
                "%d (%.1f%%)", resolved, resolvedPercentage);
        String monthlyResolvedText = String.format(Locale.getDefault(),
                "%d (%.1f%%)", monthlyResolved, monthlyResolvedPercentage);
        tvResolvedTickets.setText(resolvedText);
        tvPendingTickets.setText(String.valueOf(pending));
        tvMonthlyTickets.setText(String.valueOf(monthly));
        tvMonthlyResolved.setText(monthlyResolvedText);
        Log.d(TAG, String.format("UI actualizada:\nTotal: %d\nResueltos: %s\nPendientes: %d\n" +
                        "Mensuales: %d\nMensuales Resueltos: %s",
                total, resolvedText, pending, monthly, monthlyResolvedText));
    }
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        cardViewMonthly.setVisibility(show ? View.GONE : View.VISIBLE);
        cardViewTotal.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutCommonIncidents.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutTimeAnalysis.setVisibility(show ? View.GONE : View.VISIBLE);
        layoutWorkerPerformance.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}