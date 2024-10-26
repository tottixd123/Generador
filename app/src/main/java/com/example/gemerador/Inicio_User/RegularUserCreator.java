package com.example.gemerador.Inicio_User;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegularUserCreator {
    private static final String TAG = "RegularUserCreator";
    public static void createRegularUser(Context context, String email, String password,
                                         String nombre, String area, String cargo) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Usuarios");

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();
                    // Crear entrada en la base de datos
                    createUserInDatabase(context, usersRef, userId, email, nombre, area, cargo);
                    // Cerrar sesión después de crear el usuario
                    auth.signOut();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user: " + e.getMessage());
                    Toast.makeText(context, "Error al crear usuario: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private static void createUserInDatabase(Context context, DatabaseReference usersRef,
                                             String userId, String email, String nombre,
                                             String area, String cargo) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", nombre);
        userData.put("email", email);
        userData.put("role", "Usuario");  // Role específico para usuarios regulares
        userData.put("area", area);
        userData.put("cargo", cargo);

        usersRef.child(userId).setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Usuario creado con éxito",
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User created successfully");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error al guardar datos del usuario: " +
                            e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error saving user data: " + e.getMessage());
                });
    }
}
