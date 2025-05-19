package com.example.sleep_app.ui.fragments;

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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRelaxBinding.inflate(inflater, container, false);

        // Set click listeners on CardViews (not Buttons)
        binding.cardMeditation.setOnClickListener(v -> {
            // TODO: play meditation audio
        });

        binding.cardBreathing.setOnClickListener(v -> {
            showBreathingDialog();
        });

        binding.cardNatureSounds.setOnClickListener(v -> {
            // TODO: play relaxing nature sounds
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

}
