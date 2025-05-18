package com.example.sleep_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Log;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

import com.example.sleep_app.receiver.AlarmReceiver;
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
            calendar.set(Calendar.MILLISECOND, 0);

            time = calendar.getTimeInMillis();
            if (System.currentTimeMillis() > time) {
                time += AlarmManager.INTERVAL_DAY;
            }

            Intent intent = new Intent(this, AlarmReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "Autorise les alarmes exactes dans les paramètres", Toast.LENGTH_LONG).show();
                    Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(permissionIntent);
                    return;
                }
            }

            Log.e("AlarmDebug", "Alarm scheduled for millis: " + calendar.getTimeInMillis() +
                    ", which is in " + (calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000 + " seconds");
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);

        } else {
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
            Toast.makeText(this, "ALARM OFF", Toast.LENGTH_SHORT).show();
        }
    }

}
