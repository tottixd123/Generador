package com.example.gemerador.login;

import android.content.Intent;
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
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.MainActivity;
import com.example.gemerador.Nuevo_Registro.Nuevo_Registro;
import com.example.gemerador.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login extends AppCompatActivity {
    private EditText editTextUsuario,editTextContrasena;
    private Button buttonLogin,buttonRegister;
    private TextView textViewOlvidoContrasena;
    private ImageView backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Inicializar Firebase Auth
        mAuth=FirebaseAuth.getInstance();
        //window set up
        setupPopupWindow();
        //Inicializar views
        initializeViews();
        //detectores de cliks
        setupClickListeners();
        //tooolbar
        setupToolbar();
    }
    private void iniciarSesion() {
        String usuario = editTextUsuario.getText().toString().trim();
        String contrasena = editTextContrasena.getText().toString().trim();
        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.signInWithEmailAndPassword(usuario,contrasena)
                .addOnCompleteListener(this, task->{
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso, ahora verificamos si es admin
                        String userId = mAuth.getCurrentUser().getUid();
                        Log.d("LoginDebug", "Usuario autenticado con UID: " + userId);
                        verificarAdmin(userId);
                    }else{
                        //si el inicio de sesion falla
                        Toast.makeText(Login.this, "Error en el inicio de sesión"+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
    private void verificarAdmin(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("usuarios").child(userId);
        Log.d("LoginDebug", "Verificando admin para userId: " + userId);
        Log.d("LoginDebug", "Ruta completa: " + userRef.toString());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("LoginDebug", "onDataChange llamado");
                if (dataSnapshot.exists()) {
                    Log.d("LoginDebug", "DataSnapshot existe");
                    Log.d("LoginDebug", "Datos del usuario: " + dataSnapshot.getValue());
                    if (dataSnapshot.hasChild("esAdmin")) {
                        Boolean esAdmin = dataSnapshot.child("esAdmin").getValue(Boolean.class);
                        Log.d("LoginDebug", "esAdmin: " + esAdmin);
                        if (esAdmin != null && esAdmin) {
                            Log.d("LoginDebug", "Usuario es admin, redirigiendo a AdminMenu");
                            irAAdminMenu();
                        } else {
                            Log.d("LoginDebug", "Usuario no es admin, redirigiendo a actividad de usuario");
                            irAActividadUsuario();
                        }
                    } else {
                        Log.d("LoginDebug", "El nodo 'esAdmin' no existe para este usuario");
                        irAActividadUsuario();
                    }
                } else {
                    Log.d("LoginDebug", "DataSnapshot no existe, redirigiendo a actividad de usuario");
                    irAActividadUsuario();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("LoginDebug", "Error al verificar rol de usuario: " + databaseError.getMessage());
                Log.e("LoginDebug", "Código de error: " + databaseError.getCode());
                Log.e("LoginDebug", "Detalles: " + databaseError.getDetails());
                Toast.makeText(Login.this, "Error al verificar rol de usuario: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                irAActividadUsuario();
            }
        });
    }
    private void irAAdminMenu() {
        // Aquí puedes mostrar un menú o ir a una actividad específica para administradores
        Intent intent = new Intent(Login.this, AdminMenu.class);
        startActivity(intent);
        finish();
    }
    private void irAActividadUsuario() {
        Intent intent = new Intent(Login.this, Inicio_User.class);
        startActivity(intent);
        finish();
    }
}