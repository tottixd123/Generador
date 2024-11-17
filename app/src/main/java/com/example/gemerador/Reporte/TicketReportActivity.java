package com.example.gemerador.Reporte;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_report);

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
            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentYear = calendar.get(Calendar.YEAR);

            int totalTickets = ticketArray.length();
            int resolvedTickets = 0;
            int monthlyTickets = 0;
            int monthlyResolved = 0;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            // Debug log
            Log.d(TAG, "Total tickets encontrados: " + totalTickets);

            for (int i = 0; i < ticketArray.length(); i++) {
                JSONObject ticket = ticketArray.getJSONObject(i);

                // Verificar estado - considerar diferentes variaciones de "Resuelto"
                String status = ticket.optString("status", "").toLowerCase().trim();
                boolean isResolved = status.equals("resuelto") || status.equals("terminado") ||
                        status.equals("completed") || status.equals("resolved");

                // Debug log para cada ticket
                Log.d(TAG, String.format("Ticket #%d - Status: %s, Resuelto: %b",
                        i + 1, status, isResolved));

                if (isResolved) {
                    resolvedTickets++;
                }

                // Verificar si el ticket es del mes actual
                String creationDate = ticket.optString("creationDate", "");
                if (!creationDate.isEmpty()) {
                    try {
                        Date date = sdf.parse(creationDate);
                        if (date != null) {
                            calendar.setTime(date);
                            if (calendar.get(Calendar.MONTH) == currentMonth &&
                                    calendar.get(Calendar.YEAR) == currentYear) {

                                monthlyTickets++;
                                if (isResolved) {
                                    monthlyResolved++;
                                }

                                // Debug log para tickets mensuales
                                Log.d(TAG, String.format("Ticket mensual #%d - Creado: %s, Resuelto: %b",
                                        monthlyTickets, creationDate, isResolved));
                            }
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing date: " + creationDate + " - " + e.getMessage());
                    }
                }
            }

            // Debug log final
            Log.d(TAG, String.format("Resumen final:\nTotal: %d\nResueltos: %d\nMensuales: %d\nMensuales Resueltos: %d",
                    totalTickets, resolvedTickets, monthlyTickets, monthlyResolved));

            final int finalResolvedTickets = resolvedTickets;
            final int finalMonthlyTickets = monthlyTickets;
            final int finalMonthlyResolved = monthlyResolved;

            runOnUiThread(() -> {
                updateUI(totalTickets, finalResolvedTickets, finalMonthlyTickets, finalMonthlyResolved);
                Toast.makeText(this,
                        String.format("Total: %d, Resueltos: %d", totalTickets, finalResolvedTickets),
                        Toast.LENGTH_SHORT).show();
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
    private void updateUI(int total, int resolved, int monthly, int monthlyResolved) {
        // Asegurar que los números tengan sentido
        resolved = Math.min(resolved, total);
        monthlyResolved = Math.min(monthlyResolved, monthly);

        int pending = total - resolved;

        // Actualizar textos
        tvTotalTickets.setText(String.valueOf(total));

        // Calcular porcentajes
        float resolvedPercentage = total > 0 ? (resolved * 100f) / total : 0;
        float monthlyResolvedPercentage = monthly > 0 ? (monthlyResolved * 100f) / monthly : 0;

        // Formato con dos decimales
        String resolvedText = String.format(Locale.getDefault(),
                "%d (%.1f%%)", resolved, resolvedPercentage);
        String monthlyResolvedText = String.format(Locale.getDefault(),
                "%d (%.1f%%)", monthlyResolved, monthlyResolvedPercentage);

        tvResolvedTickets.setText(resolvedText);
        tvPendingTickets.setText(String.valueOf(pending));
        tvMonthlyTickets.setText(String.valueOf(monthly));
        tvMonthlyResolved.setText(monthlyResolvedText);

        // Debug log
        Log.d(TAG, String.format("UI actualizada:\nTotal: %d\nResueltos: %s\nPendientes: %d\n" +
                        "Mensuales: %d\nMensuales Resueltos: %s",
                total, resolvedText, pending, monthly, monthlyResolvedText));
    }
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        cardViewMonthly.setVisibility(show ? View.GONE : View.VISIBLE);
        cardViewTotal.setVisibility(show ? View.GONE : View.VISIBLE);
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