package com.example.gemerador.User_Admin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gemerador.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminUserManager extends AppCompatActivity {
    private EditText emailEditText;
    private Button promoteButton;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_manager);
        emailEditText = findViewById(R.id.emailEditText);
        promoteButton = findViewById(R.id.promoteButton);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        promoteButton.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                if (!email.isEmpty()) {
                    promoteUserToAdmin(email);
                }else {
                    Toast.makeText(AdminUserManager.this,"Por favor, ingrese un email",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void promoteUserToAdmin(final String email) {
        mDatabase.child("usuarios").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        mDatabase.child("usuarios").child(userId).child("esAdmin").setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminUserManager.this, "Usuario promovido a administrador", Toast.LENGTH_SHORT).show();
                                    Log.d("AdminUserManager", "Usuario " + email + " promovido a administrador");
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(AdminUserManager.this, "Error al promover usuario: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e("AdminUserManager", "Error al promover usuario: " + e.getMessage());
                                });
                        return; // Solo promovemos al primer usuario encontrado con ese email
                    }
                } else {
                    Toast.makeText(AdminUserManager.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                    Log.d("AdminUserManager", "Usuario con email " + email + " no encontrado");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AdminUserManager.this, "Error en la base de datos: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("AdminUserManager", "Error en la base de datos: " + databaseError.getMessage());
            }
        });
    }
}