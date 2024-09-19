package com.example.gemerador;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gemerador.Adapter.TrendsAdapter;
import com.example.gemerador.Domain.TrendsDomain;
import com.example.gemerador.Inicio_User.Inicio_User;
import com.example.gemerador.Nuevo_Registro.Nuevo_Registro;
import com.example.gemerador.login.Login;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 1;
    private RecyclerView.Adapter  adapterFppflist;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //inicio del recyclerView
        //initRecyclerView();
        // Configurar el botón de inicio de registro
        Button registro = findViewById(R.id.inicio_registro);
        registro.setOnClickListener(v -> {
            Intent intent = new Intent(this, Nuevo_Registro.class);
            startActivity(intent);});

        // Configurar el botón de inicio de sesión
        Button iniciar = findViewById(R.id.iniciar_sesion);
        iniciar.setOnClickListener(v -> {
            Intent intent = new Intent(this, Login.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
        });
    }

   private void initRecyclerView() {
        ArrayList<TrendsDomain> items = new ArrayList<>();
        items.add(new TrendsDomain("Falta de Ingeniero", "18/09/2024", "Mucha Fe en Dios", R.drawable.imagen_de_whatsapp_2024_09_18_a_las_01_26_21_d4862afa));
        items.add(new TrendsDomain("Falta de Ingeniero", "18/09/2024", "Mucha Fe en Dios", R.drawable.imagen_de_whatsapp_2024_09_18_a_las_01_26_21_d4862afa));
        items.add(new TrendsDomain("Falta de Ingeniero", "18/09/2024", "Mucha Fe en Dios", R.drawable.imagen_de_whatsapp_2024_09_18_a_las_01_26_21_d4862afa));
        items.add(new TrendsDomain("Falta de Ingeniero", "18/09/2024", "Mucha Fe en Dios", R.drawable.imagen_de_whatsapp_2024_09_18_a_las_01_26_21_d4862afa));
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adapterFppflist = new TrendsAdapter(items);
        recyclerView.setAdapter(adapterFppflist);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE ){
            if (resultCode == RESULT_OK){

            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }
}