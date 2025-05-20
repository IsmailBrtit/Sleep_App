package com.example.sleep_app.ui.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sleep_app.R;
import com.example.sleep_app.databinding.FragmentRelaxBinding;

public class RelaxFragment extends Fragment {

    private FragmentRelaxBinding binding;
    private android.app.AlertDialog breathingDialog;
    private final android.os.Handler breathingHandler = new android.os.Handler();
    private final String[] cycle = {"Inhale", "Hold", "Exhale"};
    private final int[] phaseDurations = {4000, 7000, 8000};
    private int cycleIndex = 0;
    private MediaPlayer naturePlayer;
    private android.app.AlertDialog natureSoundDialog;
    private String currentlyPlaying = "";


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRelaxBinding.inflate(inflater, container, false);


        binding.cardBreathing.setOnClickListener(v -> {
            showBreathingDialog();
        });

        binding.cardNatureSounds.setOnClickListener(v -> {
            showNatureSoundDialog();
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (naturePlayer != null && naturePlayer.isPlaying()) {
            naturePlayer.stop();
            naturePlayer.release();
            naturePlayer = null;
            currentlyPlaying = "";
        }

        binding = null;
    }

    private void showBreathingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_breathing, null);
        android.widget.TextView breathingText = view.findViewById(R.id.breathingText);

        builder.setView(view);
        builder.setCancelable(true);
        breathingDialog = builder.create();
        breathingDialog.show();

        cycleIndex = 0; // reset each time
        breathingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (breathingDialog != null && breathingDialog.isShowing()) {
                    breathingText.setText(cycle[cycleIndex]);
                    breathingHandler.postDelayed(this, phaseDurations[cycleIndex]);
                    cycleIndex = (cycleIndex + 1) % cycle.length;
                }
            }
        }, 1000);
    }

    private void showNatureSoundDialog() {
        String[] soundNames = {"Rain", "Ocean", "Forest"};
        int[] soundResIds = {R.raw.rain, R.raw.ocean, R.raw.forest};

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_nature_sounds, null);
        android.widget.TextView currentSoundText = dialogView.findViewById(R.id.currentSoundText);
        android.widget.Button stopButton = dialogView.findViewById(R.id.stopNatureSound);

        currentSoundText.setText(
                currentlyPlaying.isEmpty() ? "No sound playing" : "Playing: " + currentlyPlaying
        );

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Choose a Nature Sound");
        builder.setView(dialogView);
        builder.setItems(soundNames, (dialog, which) -> {
            if (naturePlayer != null) {
                naturePlayer.stop();
                naturePlayer.release();
            }

            naturePlayer = MediaPlayer.create(requireContext(), soundResIds[which]);
            naturePlayer.setLooping(true);
            naturePlayer.start();
            currentlyPlaying = soundNames[which];
            currentSoundText.setText("Playing: " + currentlyPlaying);
        });

        stopButton.setOnClickListener(v -> {
            if (naturePlayer != null && naturePlayer.isPlaying()) {
                naturePlayer.stop();
                naturePlayer.release();
                naturePlayer = null;
                currentlyPlaying = "";
                currentSoundText.setText("No sound playing");
            }
        });

        natureSoundDialog = builder.create();
        natureSoundDialog.show();
    }




}
