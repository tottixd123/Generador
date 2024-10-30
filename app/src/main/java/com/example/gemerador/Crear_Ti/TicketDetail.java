package com.example.gemerador.Crear_Ti;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.gemerador.R;

public class TicketDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ticket_detail);
        Toolbar toolbar=findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Detalle del Ticket");
        // Initialize views
        ImageView ivTicketImage = findViewById(R.id.ivTicketDetailImage);
        TextView tvTicketNumber = findViewById(R.id.tvTicketDetailNumber);
        TextView tvCreator = findViewById(R.id.tvTicketDetailCreator);
        TextView tvDate = findViewById(R.id.tvTicketDetailDate);
        TextView tvProblem = findViewById(R.id.tvTicketDetailProblem);
        TextView tvArea = findViewById(R.id.tvTicketDetailArea);
        TextView tvDescription = findViewById(R.id.tvTicketDetailDescription);
        // Set data from intent
        Intent intent = getIntent();
        if(intent != null){
            String ticketNumber = intent.getStringExtra("ticketNumber");
            String creator = intent.getStringExtra("creator");
            String date = intent.getStringExtra("date");
            String problem = intent.getStringExtra("problem");
            String area = intent.getStringExtra("area");
            String description = intent.getStringExtra("description");
            String imageUrl = intent.getStringExtra("imagen");
            //Set data to views
            tvTicketNumber.setText("Ticket #" + ticketNumber);
            tvCreator.setText("Creado por: " + creator);
            tvDate.setText("Fecha: " + date);
            tvProblem.setText("Problema: " + problem);
            tvArea.setText("Área: " + area);
            tvDescription.setText("Descripción:\n" + description);
            if(imageUrl != null && !imageUrl.isEmpty()){
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.bordes)
                        .error(R.drawable.bordes)
                        .into(ivTicketImage);
            }
         }
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
