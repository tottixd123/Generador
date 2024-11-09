package com.example.gemerador.Crear_Ti;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gemerador.R;

public class TicketDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ticket_detail);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle del Ticket");
        }

        // Initialize views
        ImageView ivTicketImage = findViewById(R.id.ivTicketDetailImage);
        TextView tvTicketNumber = findViewById(R.id.tvTicketDetailNumber);
        TextView tvCreator = findViewById(R.id.tvTicketDetailCreator);
        TextView tvDate = findViewById(R.id.tvTicketDetailDate);
        TextView tvProblem = findViewById(R.id.tvTicketDetailProblem);
        TextView tvArea = findViewById(R.id.tvTicketDetailArea);
        TextView tvDescription = findViewById(R.id.tvTicketDetailDescription);

        // Get data from intent
        Intent intent = getIntent();
        if (intent != null) {
            String ticketNumber = intent.getStringExtra("ticketNumber");
            String creator = intent.getStringExtra("creator");
            String date = intent.getStringExtra("date");
            String problem = intent.getStringExtra("problem");
            String area = intent.getStringExtra("area");
            String description = intent.getStringExtra("description");
            String imageBase64 = intent.getStringExtra("imagen");

            // Set text data to views
            setTextWithDefault(tvTicketNumber, "Ticket #" + ticketNumber, "Sin número");
            setTextWithDefault(tvCreator, "Creado por: " + creator, "Creador no especificado");
            setTextWithDefault(tvDate, "Fecha: " + date, "Fecha no especificada");
            setTextWithDefault(tvProblem, "Problema: " + problem, "Problema no especificado");
            setTextWithDefault(tvArea, "Área: " + area, "Área no especificada");
            setTextWithDefault(tvDescription, "Descripción:\n" + description, "Sin descripción");

            // Handle image
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                try {
                    // Decode base64 string to bitmap
                    byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    if (bitmap != null) {
                        ivTicketImage.setImageBitmap(bitmap);
                        ivTicketImage.setVisibility(View.VISIBLE);
                    } else {
                        ivTicketImage.setImageResource(R.drawable.bordes);
                        showToast("No se pudo cargar la imagen");
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    ivTicketImage.setImageResource(R.drawable.bordes);
                    showToast("Error al procesar la imagen");
                }
            } else {
                ivTicketImage.setVisibility(View.GONE);
            }
        } else {
            showToast("No se encontraron datos del ticket");
            finish();
        }
    }

    private void setTextWithDefault(TextView textView, String value, String defaultValue) {
        if (textView != null) {
            textView.setText(value != null && !value.trim().isEmpty() ? value : defaultValue);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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