package com.example.sleep_app;

import android.os.Bundle;
import android.os.Build;
import android.view.WindowManager;
import android.widget.Toast;
import android.content.DialogInterface;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.sleep_app.databinding.ActivityMainBinding;
import com.example.sleep_app.ui.fragments.HomeFragment;
import com.example.sleep_app.ui.fragments.SettingsFragment;
import com.example.sleep_app.ui.fragments.StatsFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;
//import java.util.concurrent.Executor;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;


public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // Redirige vers LoginActivity si non connecté
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        binding.bottomNavBar.setItemSelected(R.id.home, true);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frag_container_nav, new HomeFragment())
                .commit();

        binding.bottomNavBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int id) {
                Fragment fragment = null;

                if (id == R.id.home) {
                    fragment = new HomeFragment();
                } else if (id == R.id.stats) {
                    fragment = new StatsFragment();
                } else if (id == R.id.settings) {
                    fragment = new SettingsFragment();
                }

                if (fragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frag_container_nav, fragment)
                            .commit();
                }
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this, com.google.android.material.R.style.Theme_Material3_DayNight_Dialog );
        builder.setTitle("Confirm Exit");
        builder.setIcon(R.drawable.logo); // replace with your own icon
        builder.setMessage("Do you really want to exit?");
        builder.setBackground(getResources().getDrawable(R.drawable.input_background, null));
        builder.setCancelable(false);
        builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(MainActivity.this, "Exit cancelled", Toast.LENGTH_LONG).show();
            }
        });

        builder.show();
    }
}
