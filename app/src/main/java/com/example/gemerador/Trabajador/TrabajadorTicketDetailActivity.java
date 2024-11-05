package com.example.gemerador.Trabajador;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gemerador.Data_base.Ticket;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class TrabajadorTicketDetailActivity extends AppCompatActivity {
    private TextView tvTicketNumber, tvProblemType, tvArea, tvDetail, tvCreationDate;
    private TextView tvStatus, tvPriority, tvComments;
    private ImageView ivTicketImage;
    private EditText etNewComment;
    private Button btnUpdateStatus, btnAddComment;
    private Spinner spinnerStatus;

    private DatabaseReference mDatabase;
    private Ticket currentTicket;
    private String workerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trabajador_ticket_detail);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        loadTicketData();
    }

    private void initializeViews() {
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvProblemType = findViewById(R.id.tvProblemType);
        tvArea = findViewById(R.id.tvArea);
        tvDetail = findViewById(R.id.tvDetail);
        tvCreationDate = findViewById(R.id.tvCreationDate);
        tvStatus = findViewById(R.id.tvStatus);
        tvPriority = findViewById(R.id.tvPriority);
        tvComments = findViewById(R.id.tvComments);
        ivTicketImage = findViewById(R.id.ivTicketImage);
        etNewComment = findViewById(R.id.etNewComment);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnAddComment = findViewById(R.id.btnAddComment);
        spinnerStatus = findViewById(R.id.spinnerStatus);

        // Setup status spinner with valid statuses
        String[] statuses = {
                Ticket.STATUS_PENDING,
                Ticket.STATUS_IN_PROGRESS,
                Ticket.STATUS_COMPLETED
        };
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, statuses);
        spinnerStatus.setAdapter(statusAdapter);

        btnUpdateStatus.setOnClickListener(v -> updateTicketStatus());
        btnAddComment.setOnClickListener(v -> addComment());
    }

    private void loadTicketData() {
        String ticketId = getIntent().getStringExtra("ticketId");
        if (ticketId == null) {
            Toast.makeText(this, "Error: ID de ticket no válido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase.child("tickets").child(ticketId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                currentTicket = task.getResult().getValue(Ticket.class);
                if (currentTicket != null) {
                    displayTicketData();
                }
            } else {
                Toast.makeText(this, "Error al cargar el ticket", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayTicketData() {
        tvTicketNumber.setText("Ticket #" + currentTicket.getTicketNumber());
        tvProblemType.setText(currentTicket.getProblemSpinner());
        tvArea.setText(currentTicket.getArea_problema());
        tvDetail.setText(currentTicket.getDetalle());
        tvCreationDate.setText(currentTicket.getCreationDate());
        tvStatus.setText(currentTicket.getStatus());
        tvPriority.setText(currentTicket.getPriority());
        tvComments.setText(currentTicket.getComments());

        // Load ticket image if exists
        if (currentTicket.getImagen() != null && !currentTicket.getImagen().isEmpty()) {
            Picasso.get()
                    .load(currentTicket.getImagen())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(ivTicketImage);
        }

        // Set current status in spinner
        for (int i = 0; i < spinnerStatus.getAdapter().getCount(); i++) {
            if (spinnerStatus.getAdapter().getItem(i).toString().equals(currentTicket.getStatus())) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Verify if worker can modify the ticket
        boolean canModify = currentTicket.canBeModifiedBy(workerId, "Trabajador");
        btnUpdateStatus.setEnabled(canModify);
        btnAddComment.setEnabled(canModify);
        spinnerStatus.setEnabled(canModify);
    }

    private void updateTicketStatus() {
        if (currentTicket == null) return;

        String newStatus = spinnerStatus.getSelectedItem().toString();
        currentTicket.updateStatus(newStatus, workerId, mDatabase);
        Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show();
    }

    private void addComment() {
        if (currentTicket == null) return;

        String comment = etNewComment.getText().toString().trim();
        if (comment.isEmpty()) {
            Toast.makeText(this, "El comentario no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get worker name from shared preferences or database
        String workerName = getSharedPreferences("UserData", MODE_PRIVATE)
                .getString("nombre", "Trabajador");

        currentTicket.addComment(comment, workerId, workerName, mDatabase);
        etNewComment.setText("");
        Toast.makeText(this, "Comentario agregado", Toast.LENGTH_SHORT).show();
    }
}