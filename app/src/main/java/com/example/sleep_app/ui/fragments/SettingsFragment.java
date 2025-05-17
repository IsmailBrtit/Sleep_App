package com.example.sleep_app.ui.fragments;

import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.example.sleep_app.LoginActivity;
import com.example.sleep_app.R;


public class SettingsFragment extends Fragment {
    

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        Button selectRingtoneButton = view.findViewById(R.id.selectRingtoneButton);
        selectRingtoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Sélectionnez votre sonnerie de réveil");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_ALARM_ALERT_URI);
            startActivityForResult(intent, 999);
        });

        Switch switchSmartAlarm = view.findViewById(R.id.switchSmartAlarm);

        // Charger la valeur actuelle depuis les préférences
        SharedPreferences prefs = requireContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("smart_alarm_enabled", true);
        switchSmartAlarm.setChecked(isEnabled);

        // Sauvegarder la nouvelle valeur si l'utilisateur modifie le switch
        switchSmartAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("smart_alarm_enabled", isChecked);
            editor.apply();

            String message = isChecked ? "Réveil intelligent activé" : "Réveil intelligent désactivé";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 999 && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                SharedPreferences.Editor editor = getContext()
                        .getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE)
                        .edit();
                editor.putString("ringtone_uri", uri.toString());
                editor.apply();

                Toast.makeText(getContext(), "Sonnerie enregistrée !", Toast.LENGTH_SHORT).show();
            }
        }
    }



}

