package com.example.gemerador.Admin;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminAuthManager {
    private static final String TAG = "AdminAuthManager";
    private static AdminAuthManager instance;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private Boolean isAdminVerified = null;

    private AdminAuthManager() {
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Usuarios");
    }

    public static synchronized AdminAuthManager getInstance() {
        if (instance == null) {
            instance = new AdminAuthManager();
        }
        return instance;
    }

    public void verifyAdminAccess(OnAdminVerificationListener listener) {
        // Si ya verificamos que es admin, retornamos el resultado cached
        if (isAdminVerified != null) {
            listener.onVerificationComplete(isAdminVerified);
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            isAdminVerified = false;
            listener.onVerificationComplete(false);
            return;
        }

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    isAdminVerified = "Administrador".equals(role);
                    listener.onVerificationComplete(isAdminVerified);
                } else {
                    isAdminVerified = false;
                    listener.onVerificationComplete(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isAdminVerified = false;
                listener.onVerificationComplete(false);
            }
        });
    }

    public void clearAdminVerification() {
        isAdminVerified = null;
    }

    public interface OnAdminVerificationListener {
        void onVerificationComplete(boolean isAdmin);
    }
}