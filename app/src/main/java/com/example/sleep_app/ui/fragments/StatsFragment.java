package com.example.sleep_app.ui.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.sleep_app.databinding.FragmentStatsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private final List<String> days = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        loadSleepChart();
        loadHabitsChart();
        return binding.getRoot();
    }

    private void loadSleepChart() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String raw = documentSnapshot.getString("7days");
                    if (raw == null) return;
                    String[] parts = raw.split(";");

                    ArrayList<BarEntry> entries = new ArrayList<>();
                    for (int i = 0; i < parts.length; i++) {
                        float percent = Float.parseFloat(parts[i]);
                        float hours = (percent * 8f) / 100f;
                        entries.add(new BarEntry(i, hours));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Sleep Duration (hrs)");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    dataSet.setValueTextColor(Color.BLACK);
                    dataSet.setValueTextSize(12f);

                    BarData data = new BarData(dataSet);
                    data.setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int h = (int) value;
                            int m = (int) ((value - h) * 60);
                            return h + "h " + m + "m";
                        }
                    });

                    BarChart chart = binding.sleepChart;
                    chart.setData(data);

                    Description desc = new Description();
                    desc.setText("Weekly Sleep Tracker");
                    desc.setTextSize(16f);
                    chart.setDescription(desc);

                    chart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            if ((int) value >= 0 && (int) value < days.size()) {
                                return days.get((int) value);
                            } else return "";
                        }
                    });

                    chart.animateY(1000);
                    chart.invalidate();
                });
    }

    private void loadHabitsChart() {
        String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        List<String> habits = Arrays.asList("Caffeine", "Stress", "Exercise");
        int[] values = new int[habits.size()];
        final int[] validCount = {0};

        FirebaseFirestore.getInstance().collection("Users")
                .document(uid)
                .collection("SleepRecords")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        boolean counted = false;

                        String caffeine = doc.getString("caffeine");
                        if (caffeine != null && (caffeine.equalsIgnoreCase("A little") || caffeine.equalsIgnoreCase("A lot"))) {
                            values[0]++;
                            counted = true;
                        }

                        String stress = doc.getString("stress");
                        if (stress != null && stress.equalsIgnoreCase("High")) {
                            values[1]++;
                            counted = true;
                        }

                        String exercise = doc.getString("exercise");
                        if (exercise != null && exercise.equalsIgnoreCase("Yes")) {
                            values[2]++;
                            counted = true;
                        }

                        if (counted) validCount[0]++;
                    }

                    ArrayList<BarEntry> entries = new ArrayList<>();
                    for (int i = 0; i < values.length; i++) {
                        float percent = (validCount[0] == 0) ? 0 : (values[i] * 100f) / validCount[0];
                        entries.add(new BarEntry(i, percent));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Habit Frequencies (%)");
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                    dataSet.setValueTextColor(Color.BLACK);
                    dataSet.setValueTextSize(12f);

                    BarData data = new BarData(dataSet);
                    BarChart chart = binding.habitsChart;
                    chart.setData(data);

                    Description desc = new Description();
                    desc.setText("Weekly Habit Trends");
                    desc.setTextSize(16f);
                    chart.setDescription(desc);

                    chart.getXAxis().setGranularity(1f);
                    chart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getFormattedValue(float value) {
                            int index = (int) value;
                            if (index >= 0 && index < habits.size()) return habits.get(index);
                            else return "";
                        }
                    });

                    chart.animateY(1000);
                    chart.invalidate();
                });
    }
}
