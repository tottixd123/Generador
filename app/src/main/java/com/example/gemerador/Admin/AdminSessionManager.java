package com.example.gemerador.Admin;
import androidx.annotation.NonNull;

import com.example.gemerador.Models.Solicitud;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminSessionManager {
    private static final String TAG = "AdminSessionManager";
    private DatabaseReference solicitudesRef;
    private ValueEventListener solicitudesListener;
    private FirebaseAuth mAuth;

    public AdminSessionManager() {
        mAuth = FirebaseAuth.getInstance();
        solicitudesRef = FirebaseDatabase.getInstance().getReference().child("solicitudes_registro");
    }

    public void startListening(@NonNull OnSolicitudesLoadListener listener) {
        if (mAuth.getCurrentUser() != null) {
            solicitudesListener = solicitudesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    List<Solicitud> solicitudes = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Solicitud solicitud = snapshot.getValue(Solicitud.class);
                        if (solicitud != null) {
                            solicitudes.add(solicitud);
                        }
                    }

                    listener.onSolicitudesLoaded(solicitudes);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    listener.onError(databaseError.getMessage());
                }
            });
        }
    }

    public void stopListening() {
        if (solicitudesListener != null) {
            solicitudesRef.removeEventListener(solicitudesListener);
            solicitudesListener = null;
        }
    }

    public void signOut(OnSignOutCompleteListener listener) {
        stopListening();
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
            AdminAuthManager.getInstance().clearAdminVerification();
        }
        if (listener != null) {
            listener.onSignOutComplete();
        }
    }

    public interface OnSolicitudesLoadListener {
        void onSolicitudesLoaded(List<Solicitud> solicitudes);
        void onError(String error);
    }

    public interface OnSignOutCompleteListener {
        void onSignOutComplete();
    }
}