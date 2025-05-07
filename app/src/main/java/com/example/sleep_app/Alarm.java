package com.example.sleep_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import com.example.sleep_app.alarmBroadcastReceiver.AlarmReceiver;
import com.example.sleep_app.databinding.ActivityAlarmBinding;

public class Alarm extends AppCompatActivity {
    ActivityAlarmBinding binding;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlarmBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        binding.toggleButton.setOnClickListener(v -> OnToggleClicked(binding.toggleButton));
    }

    public void OnToggleClicked(View view) {
        long time;
        if (((ToggleButton) view).isChecked()) {
            Toast.makeText(this, "ALARM ON", Toast.LENGTH_SHORT).show();

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, binding.timePicker.getHour());
            calendar.set(Calendar.MINUTE, binding.timePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);

            Intent intent = new Intent(this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            time = calendar.getTimeInMillis();
            if (System.currentTimeMillis() > time) {
                time += AlarmManager.INTERVAL_DAY;
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, time, 10000, pendingIntent);
        } else {
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
            Toast.makeText(this, "ALARM OFF", Toast.LENGTH_SHORT).show();
        }
    }
}
