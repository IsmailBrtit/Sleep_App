package com.example.sleep_app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sleep_app.databinding.FragmentRelaxBinding;

public class RelaxFragment extends Fragment {

    private FragmentRelaxBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRelaxBinding.inflate(inflater, container, false);

        // Set click listeners on CardViews (not Buttons)
        binding.cardMeditation.setOnClickListener(v -> {
            // TODO: play meditation audio
        });

        binding.cardBreathing.setOnClickListener(v -> {
            // TODO: start breathing guide
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
}
