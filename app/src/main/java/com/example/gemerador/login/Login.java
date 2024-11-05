package com.example.gemerador.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gemerador.Admin.AdminMenu;
import com.example.gemerador.Gestion.GestionTickets;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.MainActivity;
import com.example.gemerador.Nuevo_Registro.Nuevo_Registro;
import com.example.gemerador.R;
import com.example.gemerador.Trabajador.TrabajadorMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText editTextUsuario, editTextContrasena;
    private Button buttonLogin, buttonRegister;
    private TextView textViewOlvidoContrasena;
    private ImageView backButton;
    public FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Inicializar Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Usuarios");
        //window set up
        setupPopupWindow();
        //Inicializar views
        initializeViews();
        //detectores de cliks
        setupClickListeners();
        //tooolbar
        setupToolbar();
    }
    public void iniciarSesion() {
        String email = editTextUsuario.getText().toString().trim();
        String password = editTextContrasena.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Intentando iniciar sesión con email: " + email); // Nuevo log

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Login successful for user: " + user.getEmail());
                            verificarRolUsuario(user.getUid());
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        // Mensaje de error más específico
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Error desconocido";
                        Toast.makeText(Login.this,
                                "Error de autenticación: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    private void setupPopupWindow() {
        DisplayMetrics displayMetrics=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width=(int)(displayMetrics.widthPixels * 0.9);
        int height=(int)(displayMetrics.heightPixels * 0.7);
        getWindow().setLayout(width,height);
        getWindow().setGravity(android.view.Gravity.CENTER);
        // añade animacion
        getWindow().setWindowAnimations(R.style.PopupAnimation);
    }
    private void initializeViews() {
        editTextUsuario=findViewById(R.id.editTextText);
        editTextContrasena=findViewById(R.id.editTextTextPassword);
        buttonLogin=findViewById(R.id.btn_inicio);
        buttonRegister=findViewById(R.id.button2);
        textViewOlvidoContrasena=findViewById(R.id.textView_olvido_contrasena);
        backButton = findViewById(R.id.atras_soli);
    }
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> volverAlMain());
        buttonLogin.setOnClickListener(v -> iniciarSesion());
        buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, Nuevo_Registro.class);
            startActivity(intent);
        });
        textViewOlvidoContrasena.setOnClickListener(v -> {
            String usuario = editTextUsuario.getText().toString().trim();
            if (usuario.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese su correo electrónico", Toast.LENGTH_SHORT).show();
            } else {
                // Por ahora, solo mostraremos un mensaje
                Toast.makeText(this, "Se ha enviado un correo para restablecer su contraseña", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void volverAlMain() {
        Intent intent = new Intent(Login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }
    private void verificarRolUsuario(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Usuarios").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Obtener el rol del usuario
                    String role = dataSnapshot.child("role").getValue(String.class);
                    String nombre = dataSnapshot.child("nombre").getValue(String.class);

                    // Guardar información del usuario en SharedPreferences
                    guardarDatosUsuario(nombre, role);

                    switch (role) {
                        case "Administrador":
                            irAAdminMenu();
                            break;
                        case "Usuario":
                            irAInicioUser();
                            break;
                        case "Trabajador":
                            irATrabajadorMenu();
                            break;
                        default:
                            Toast.makeText(Login.this, "Rol de usuario no válido", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Login.this, "Error al verificar usuario", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error: " + error.getMessage());
            }
        });
    }
    private void guardarDatosUsuario(String nombre, String role) {
        SharedPreferences prefs = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nombre", nombre);
        editor.putString("role", role);
        editor.apply();
    }

    private void irAAdminMenu() {
        Intent intent = new Intent(Login.this, AdminMenu.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irAInicioUser() {
        Intent intent = new Intent(Login.this, Inicio_User.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    private void irATrabajadorMenu() {
        Intent intent = new Intent(Login.this, TrabajadorMenu.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}