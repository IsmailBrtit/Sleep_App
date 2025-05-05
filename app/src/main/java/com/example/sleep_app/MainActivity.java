package com.example.sleep_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import android.util.Log;

import java.util.concurrent.Executor;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔥 Initialise Firebase manuellement ici
        FirebaseApp.initializeApp(this);

        FirebaseAuth.getInstance().signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d("FIREBASE_TEST", "Connexion Firebase réussie ✅");
                    } else {
                        Log.e("FIREBASE_TEST", "Erreur de connexion ❌", task.getException());
                    }
                });

    }
}
