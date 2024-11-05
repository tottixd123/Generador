package com.example.gemerador.Trabajador;

import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

public class TrabajadorService {
    private final DatabaseReference ticketsRef;
    private final String trabajadorId;
    private ValueEventListener ticketsListener;

    // Interface unificada para callbacks de tickets
    public interface OnTicketsLoadedListener {
        void onTicketsLoaded(List<Ticket> tickets);
        void onError(String error);
    }

    public interface OnOperationCompleteListener {
        void onSuccess();
        void onError(String error);
    }

    public TrabajadorService(String trabajadorId) {
        this.trabajadorId = trabajadorId;
        this.ticketsRef = FirebaseDatabase.getInstance().getReference("tickets");
    }

    public void loadAssignedTickets(final OnTicketsLoadedListener listener) {
        ticketsListener = ticketsRef.orderByChild("assignedWorkerId").equalTo(trabajadorId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<Ticket> tickets = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Ticket ticket = snapshot.getValue(Ticket.class);
                            if (ticket != null) {
                                tickets.add(ticket);
                            }
                        }
                        listener.onTicketsLoaded(tickets);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onError(databaseError.getMessage());
                    }
                });
    }

    public void updateTicketStatus(String ticketId, String newStatus, OnOperationCompleteListener listener) {
        ticketsRef.child(ticketId).child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void addTicketComment(String ticketId, String comment, OnOperationCompleteListener listener) {
        String commentKey = ticketsRef.child(ticketId).child("comments").push().getKey();
        if (commentKey != null) {
            ticketsRef.child(ticketId).child("comments").child(commentKey).setValue(comment)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }

    public void cleanup() {
        if (ticketsListener != null) {
            ticketsRef.removeEventListener(ticketsListener);
        }
    }
}