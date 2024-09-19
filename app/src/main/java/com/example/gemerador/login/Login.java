package com.example.gemerador.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.Nuevo_Registro.Nuevo_Registro;
import com.example.gemerador.R;

public class Login extends AppCompatActivity {
    private EditText editTextUsuario,editTextContrasena;
    private Button buttonLogin,buttonRegister;
    private TextView textViewOlvidoContrasena;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //window set up
        setupPopupWindow();
        //Inicializar views
        initializeViews();
        //detectores de cliks
        setupClickListeners();
        //tooolbar
        setupToolbar();
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
    }
    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            String usuario = editTextUsuario.getText().toString();
            String contrasena = editTextContrasena.getText().toString();
            if (usuario.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, Inicio_User.class);
                startActivity(intent);
                finish();
            }
        });

        buttonRegister.setOnClickListener(v -> {
            Button Soli = findViewById(R.id.button2);
             Soli.setOnClickListener(v1 -> {
                Intent intent = new Intent(this, Nuevo_Registro.class);
                startActivity(intent);
            });

        });
        textViewOlvidoContrasena.setOnClickListener(v -> {
            Toast.makeText(this, "Función de recuperación de contraseña no implementada", Toast.LENGTH_SHORT).show();
        });
    }
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);  // Hide default title
        }
    }
}