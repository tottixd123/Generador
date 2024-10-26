package com.example.gemerador;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Configuramos la persistencia de Firebase antes de cualquier otro uso
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
