package com.example.gemerador.Trabajador;


import androidx.annotation.NonNull;

import com.example.gemerador.Data_base.Ticket;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class TrabajadorSessionManager {
    private String userId;
    private DatabaseReference mDatabase;
    private ValueEventListener ticketsListener;

    public TrabajadorSessionManager(String userId) {
        this.userId = userId;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void getAssignedTickets(TicketsLoadCallback callback) {
        DatabaseReference ticketsRef = mDatabase.child("tickets");
        ticketsListener = ticketsRef.orderByChild("assignedTo").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Ticket> tickets = new ArrayList<>();
                        for (DataSnapshot ticketSnap : snapshot.getChildren()) {
                            Ticket ticket = ticketSnap.getValue(Ticket.class);
                            if (ticket != null) {
                                ticket.setId(ticketSnap.getKey());
                                tickets.add(ticket);
                            }
                        }
                        callback.onTicketsLoaded(tickets);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void updateTicketStatus(String ticketId, String newStatus, StatusUpdateCallback callback) {
        mDatabase.child("tickets").child(ticketId).child("status")
                .setValue(newStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void signOut() {
        if (ticketsListener != null) {
            mDatabase.child("tickets").removeEventListener(ticketsListener);
        }
    }

    public interface TicketsLoadCallback {
        void onTicketsLoaded(List<Ticket> tickets);
        void onError(String error);
    }

    public interface StatusUpdateCallback {
        void onSuccess();
        void onError(String error);
    }
}
