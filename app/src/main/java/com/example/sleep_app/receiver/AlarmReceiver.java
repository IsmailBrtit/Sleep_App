package com.example.sleep_app.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.sleep_app.MainActivity;
import com.example.sleep_app.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    public static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("AlarmReceiver", "TEST: Broadcast received!");


        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "SleepApp::AlarmWakeLock");
        wakeLock.acquire(5000);

        Uri alarmUri = intent.getParcelableExtra("ringtoneUri");
        if (alarmUri == null) {
            alarmUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            Log.d("AlarmReceiver", "Alarm URI: " + alarmUri);
            Log.w("AlarmReceiver", "No custom ringtone passed, using default.");
        }

        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);

            if (AlarmReceiver.mediaPlayer != null) {
                AlarmReceiver.mediaPlayer.release();
            }

            AlarmReceiver.mediaPlayer = new MediaPlayer();
            AlarmReceiver.mediaPlayer.setDataSource(context, alarmUri);
            AlarmReceiver.mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            AlarmReceiver.mediaPlayer.setLooping(true);
            AlarmReceiver.mediaPlayer.prepare();
            AlarmReceiver.mediaPlayer.start();

            Log.d("AlarmReceiver", "🔊 Alarm sound started");
        } catch (Exception e) {
            Log.e("AlarmReceiver", "⚠️ Failed to set volume", e);
        }


        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "alarm_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Alarm Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.clock)
                .setContentTitle("⏰ Réveil intelligent")
                .setContentText("Votre alarme est déclenchée")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(new long[]{0, 1000, 1000, 1000})
                .setAutoCancel(true);

        manager.notify(202, builder.build());

        Toast.makeText(context, "⏰ Réveil  activé !", Toast.LENGTH_LONG).show();

        SharedPreferences prefs = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        edit.putString("name", null);
        edit.putString("age", null);
        edit.putString("wakeUp", now);
        edit.apply();

        context.sendBroadcast(new Intent("com.example.sleep_app.ALARM_TRIGGERED"));


        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(launchIntent);

    }
}
