package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Oculta la barra de acción si está presente
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Temporizador de 2 segundos antes de ir a MainActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish(); // Cierra SplashActivity para que no regrese al presionar atrás
        }, 2000); // 2000 ms = 2 segundos
    }
}
