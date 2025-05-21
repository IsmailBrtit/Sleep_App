package com.example.sleep_app.ui.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;


import android.Manifest;
import com.example.sleep_app.receiver.AlarmReceiver;
import com.example.sleep_app.receiver.DisableDndReceiver;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.Build;
import com.google.firebase.firestore.SetOptions;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.example.sleep_app.ProfileActivity;
import com.example.sleep_app.R;
import com.example.sleep_app.databinding.FragmentHomeBinding;
import com.example.sleep_app.model.Users;


public class HomeFragment extends Fragment {

    FragmentHomeBinding binding;
    Thread thread;
    String userID;
    SharedPreferences sharedPreferences;
    ProgressDialog progressDialog;
    volatile boolean countSec = true;
    private Handler countdownHandler = new Handler();
    private Runnable countdownRunnable;
    private static final int NOTIF_ID = 123;
    private static final int ANDROID_13 = 33; // TIRAMISU
    private boolean isAlarmReceiverRegistered = false;




    int sleepHour = -1, sleepMinute = -1;
    int wakeHour = -1, wakeMinute = -1;

    public HomeFragment(){}

    private final BroadcastReceiver alarmTriggeredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("SleepApp", "Received alarm broadcast. Checking if sleep duration was short...");

            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String sleepAt = sharedPreferences.getString("sleepAt", "");

            Timestamp start = stringToTimestamp(sharedPreferences.getString("age", ""));
            Timestamp end = stringToTimestamp(timeStamp);

            if (start == null || end == null) return;

            long durationMillis = end.getTime() - start.getTime();
            long durationMinutes = durationMillis / 1000 / 60;

            Log.d("SleepApp", "Sleep duration: " + durationMinutes + " min");

            //If total sleep was less than 5 minutes, disable DND to hear alarm clearly
            if (durationMinutes < 5) {
                Log.d("SleepApp", "Sleep was too short. Disabling DND early so alarm is audible.");
                toggleDoNotDisturb(false);
            }

            // 🟡 Continue normal end-of-sleep logic
            Map<String, Object> recordMap = new HashMap<>();
            recordMap.put("sleepAt", sleepAt);
            recordMap.put("wakeUp", timeStamp);
            recordMap.put("duration", (durationMinutes / 60) + "h " + (durationMinutes % 60) + "min");

            String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(userID)
                    .collection("SleepRecords")
                    .document(todayDate)
                    .set(recordMap, SetOptions.merge());

            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.putString("name", null);
            myEdit.putString("age", null);
            myEdit.putString("wakeUp", timeStamp);
            myEdit.apply();

            countSec = false;
            if (thread != null) thread.interrupt();
            setWakeTime();
            cancelCountdownNotification();
            getOperationLocally();
        }
    };



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home,container,false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "alarm_channel", "Alarm Notifications", NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }


        binding = FragmentHomeBinding.bind(view);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        sharedPreferences = getContext().getSharedPreferences("MySharedPref",getContext().MODE_PRIVATE);

        setSleepTime();
        setWakeTime();
        avg_sleep();
        updateRecommendation();


        binding.addSleepExtra.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ExtraSleepAddFunction();
            }
        });
        binding.sleepScoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        dataRetriveFromFirebase();

        binding.userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ProfileActivity.class);
                startActivity(intent);
            }
        });

        binding.alarmImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openClockIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                openClockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(openClockIntent);
            }
        });

        getOperationLocally();




        //start sleep
        binding.startButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (sharedPreferences.getString("name","").equals("")) {
                    showPreSleepHabitDialog(() -> {
                        SharedPreferences.Editor myEdit = sharedPreferences.edit();
                        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                        myEdit.putString("name", "active");
                        myEdit.putString("age", timeStamp);
                        myEdit.putString("sleepAt", timeStamp);
                        myEdit.putString("wakeUp", "");
                        myEdit.commit();

                        setSleepTime();
                        countSec = true;
                        toggleDoNotDisturb(true);// ENABLE DND when starting sleep
                        getOperationLocally();
                    });
                }
                else {
                    AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(requireContext(), AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    alarmManager.cancel(pendingIntent);

                    // Stop dynamic notification countdown
                    cancelCountdownNotification();

                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    String sleepAt = sharedPreferences.getString("sleepAt", "");

                    Timestamp date_1 = stringToTimestamp(sharedPreferences.getString("age",""));
                    Timestamp date_2 = stringToTimestamp(timeStamp);

                    if (date_1 == null || date_2 == null) {
                        Toast.makeText(getContext(), "Données manquantes pour calculer la durée", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long milliseconds = date_2.getTime() - date_1.getTime();
                    String todayDateFirestore = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    String todayDateForWin = new SimpleDateFormat("dd-M-yyyy").format(new Date());

                    upDateWinNode(milliseconds, todayDateForWin);

                    long hours = (milliseconds / 1000) / 3600;
                    long minutes = ((milliseconds / 1000) / 60) % 60;
                    String duration = hours + "h " + minutes + "min";

                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(userID)
                            .collection("SleepRecords")
                            .document(todayDateFirestore)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                long totalMillis = milliseconds;

                                if (documentSnapshot.exists()) {
                                    String prevDuration = documentSnapshot.getString("duration");
                                    if (prevDuration != null && prevDuration.contains("h")) {
                                        String[] parts = prevDuration.split("h|min");
                                        try {
                                            int prevH = Integer.parseInt(parts[0].trim());
                                            int prevM = Integer.parseInt(parts[1].trim());
                                            totalMillis += (prevH * 3600L + prevM * 60L) * 1000;
                                        } catch (Exception e) {
                                            Log.e("SleepApp", "Parsing error in previous duration", e);
                                        }
                                    }
                                }

                                long finalH = (totalMillis / 1000) / 3600;
                                long finalM = ((totalMillis / 1000) / 60) % 60;
                                String finalDuration = finalH + "h " + finalM + "min";

                                Map<String, Object> recordMap = new HashMap<>();
                                recordMap.put("sleepAt", sleepAt);
                                recordMap.put("wakeUp", timeStamp);
                                recordMap.put("duration", finalDuration);

                                FirebaseFirestore.getInstance()
                                        .collection("Users")
                                        .document(userID)
                                        .collection("SleepRecords")
                                        .document(todayDateFirestore)
                                        .set(recordMap, SetOptions.merge());
                            });



                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("name", null);
                    myEdit.putString("age", null);
                    myEdit.putString("wakeUp", timeStamp);
                    myEdit.commit();

                    setWakeTime();
                    countSec = false;
                    if (thread != null) thread.interrupt();
                    toggleDoNotDisturb(false);// DISABLE DND when ending sleep
                    getOperationLocally();
                }
            }
        });



        IntentFilter filter = new IntentFilter("com.example.sleep_app.ALARM_TRIGGERED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26+
            requireContext().registerReceiver(alarmTriggeredReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            ContextCompat.registerReceiver(requireContext(), alarmTriggeredReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        isAlarmReceiverRegistered = true;

        return view;
    }

    private void setWakeTime() {
        try{
            String wake = sharedPreferences.getString("wakeUp","");
            Timestamp timecurr = stringToTimestamp(wake);

            long hour = Objects.requireNonNull(timecurr).getHours();
            long min = timecurr.getMinutes();
            wakeHour = (int) hour;
            wakeMinute = (int) min;
            sharedPreferences.edit()
                    .putInt("wakeHour", wakeHour)
                    .putInt("wakeMinute", wakeMinute)
                    .apply();
            String am_pm ;
            if(hour>=12) am_pm = "pm";
            else am_pm = "am";
            hour = hour%12;
            formatTime(hour,min, binding.wakeUpTimeHintTime,am_pm);
        }catch (Exception e){
            binding.wakeUpTimeHintTime.setText("--:--");
        }
    }

    private void formatTime(long hour, long min,TextView txt,String am_pm){
        if(hour>=10 && min>=10) txt.setText(hour+":"+min+" "+am_pm);
        else if(hour<10 && min>=10) txt.setText("0"+hour+":"+min+" "+am_pm);
        else if(hour>=10 && min<10) txt.setText(hour+":"+"0"+min+" "+am_pm);
        else txt.setText("0"+hour+":"+"0"+min+" "+am_pm);
    }

    private void setSleepTime() {
        try {
            String sleep = sharedPreferences.getString("sleepAt", "");
            Timestamp sleepTimeStamp = stringToTimestamp(sleep);
            long hour = Objects.requireNonNull(sleepTimeStamp).getHours();
            long min = sleepTimeStamp.getMinutes();
            String am_pm ;
            if(hour>=12) am_pm = "pm";
            else am_pm = "am";
            hour = hour % 12;
            formatTime(hour,min, binding.SleepAtTimeTime,am_pm);
            //binding.wakeUpTimeText.setText("--:--");
        }catch (Exception e){
            binding.SleepAtTimeTime.setText("--:--");
        }
    }

    private void ExtraSleepAddFunction() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.time_pick_dialog,null);
        AlertDialog timePickerDialog = new AlertDialog.Builder(getContext())
                .setTitle("Set Time")
                .setView(v)
                .setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (sleepHour != -1 && wakeHour != -1) {
                            Log.d("AlarmDebug", "Set sleepHour=" + sleepHour + ", sleepMinute=" + sleepMinute +
                                    ", wakeHour=" + wakeHour + ", wakeMinute=" + wakeMinute);

                            long extraMillis = (Math.abs(sleepHour - wakeHour) * 60L + Math.abs(sleepMinute - wakeMinute)) * 60 * 1000;
                            String todayDateFirestore = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                            FirebaseFirestore.getInstance()
                                    .collection("Users")
                                    .document(userID)
                                    .collection("SleepRecords")
                                    .document(todayDateFirestore)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String existingDuration = documentSnapshot.getString("duration");
                                            long totalMillis = extraMillis;

                                            if (existingDuration != null && existingDuration.contains("h")) {
                                                String[] parts = existingDuration.split("h|min");
                                                int hrs = Integer.parseInt(parts[0].trim());
                                                int mins = Integer.parseInt(parts[1].trim());
                                                totalMillis += (hrs * 3600L + mins * 60L) * 1000;
                                            }

                                            long hours = (totalMillis / 1000) / 3600;
                                            long minutes = ((totalMillis / 1000) / 60) % 60;

                                            String newDuration = hours + "h " + minutes + "min";

                                            Map<String, Object> updateMap = new HashMap<>();
                                            updateMap.put("duration", newDuration);

                                            FirebaseFirestore.getInstance()
                                                    .collection("Users")
                                                    .document(userID)
                                                    .collection("SleepRecords")
                                                    .document(todayDateFirestore)
                                                    .update(updateMap)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(getContext(), "Extra nap added!", Toast.LENGTH_SHORT).show();
                                                        Log.d("SleepApp", "Updated duration to: " + newDuration);
                                                    });
                                        } else {
                                            Toast.makeText(getContext(), "No sleep data for today found.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        } else {
                            Toast.makeText(getContext(), "Kindly set valid sleep and wake up time!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).create();
        timePickerDialog.show();
        TextView setSleepTimeTv = (TextView) v.findViewById(R.id.setSleepTimeTv);
        TextView setWakeTimeTv = (TextView) v.findViewById(R.id.setWakeTimeTv);

        v.findViewById(R.id.setSleepTimeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                        android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int sleepHourT, int sleepMinuteT) {
                                sleepHour = sleepHourT;
                                sleepMinute = sleepMinuteT;
                                String am_pm ;
                                if(sleepHourT>=12) am_pm = "pm";
                                else am_pm = "am";
                                formatTime(sleepHourT,sleepMinuteT,setSleepTimeTv,am_pm);
                            }
                        }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY),Calendar.getInstance().get(Calendar.MINUTE),false);

                timePickerDialog.show();
            }
        });

        v.findViewById(R.id.setWakeTimeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog1 = new TimePickerDialog(getContext(),
                        android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int wakeHourT, int wakeMinuteT) {
                                wakeHour = wakeHourT;
                                wakeMinute = wakeMinuteT;
                                String am_pm ;
                                if(wakeHour>=12) am_pm = "pm";
                                else am_pm = "am";
                                formatTime(wakeHour,wakeMinute,setWakeTimeTv,am_pm);
                            }
                        }, Calendar.getInstance().get(Calendar.HOUR_OF_DAY),Calendar.getInstance().get(Calendar.MINUTE),false);
                timePickerDialog1.show();
            }
        });
    }

    private void upDateWinNode(long milliseconds, String today_date) {
        double sec = milliseconds / 1000.0;
        double percentage = (sec / 86400.0) * 100.0;
        double finalSec = Math.round(percentage * 100.0) / 100.0;

        FirebaseFirestore.getInstance().collection("Users").document(userID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String s = documentSnapshot.getString("7days");
                    if (s == null) s = "0;0;0;0;0;0;0";
                    String[] sp = s.split(";");
                    String[] updated = new String[7];

                    // Shift left
                    for (int i = 1; i < 7; i++) {
                        updated[i - 1] = sp[i];
                    }
                    updated[6] = String.valueOf(finalSec); // new value

                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < 6; i++) result.append(updated[i]).append(";");
                    result.append(updated[6]);

                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("7days", result.toString());

                    FirebaseFirestore.getInstance().collection("Users")
                            .document(userID)
                            .update(updateMap)
                            .addOnSuccessListener(aVoid -> {
                                avg_sleep(); // update UI
                                updateRecommendation(); // refresh rec
                            });
                });
    }



    private void getOperationLocally() {
        if (sharedPreferences.getString("name","").equals("")){
            binding.activeSleepLayout.setVisibility(View.GONE);
            binding.startButton.setText("Start Sleep");
        }else{
            timeCountSetText();
            avg_sleep();
            binding.activeSleepLayout.setVisibility(View.VISIBLE);
            binding.startButton.setText("End Sleep");
        }

    }

    private void timeCountSetText() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (countSec){
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(!countSec){
                                return;
                            }
                            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());


                            Timestamp date_1 = stringToTimestamp(sharedPreferences.getString("age",""));
                            Timestamp date_2 = stringToTimestamp(timeStamp);
                            if (date_1 == null || date_2 == null) {
                                Log.e("SleepApp", "⛔ date_1 or date_2 is null, skipping countdown update");
                                return;
                            }

                            long milliseconds = date_2.getTime() - date_1.getTime();
                            long hour = (milliseconds/1000)/3600;
                            long min = ((milliseconds/1000)/60)%60;
                            long sec = (milliseconds/1000)%60;

                            if(hour<24) binding.timeCount.setText(hour+" hr  "+ min+" min "+ sec+" sec");
                            else binding.timeCount.setText("24 hr+");
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();


    }


    private Timestamp stringToTimestamp(String date) {
        try {
            if (date == null || date.isEmpty()) return null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsedDate = dateFormat.parse(date);
            return new Timestamp(parsedDate.getTime());
        } catch (Exception e) {
            Log.e("TimestampError", "Invalid date string: " + date);
            return null;
        }
    }


    private void dataRetriveFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Users userprofile = documentSnapshot.toObject(Users.class);

                        if (userprofile != null) {
                            String fullname = userprofile.name;
                            if (fullname.contains(" ")) {
                                fullname = fullname.substring(0, fullname.indexOf(" "));
                            }
                            binding.mainActUserName.setText(fullname + " !");
                        }

                        Object pfpUrl = documentSnapshot.get("user_image");
                        if (pfpUrl != null) {
                            Picasso.get()
                                    .load(pfpUrl.toString())
                                    .placeholder(R.drawable.profile)
                                    .error(R.drawable.profile)
                                    .into(binding.userProfileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Cannot fetch data", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDialog(){
        new AlertDialog.Builder(requireContext())
                .setTitle("Sleep Score Card")
                .setMessage("Excellent: 85–100%\nGood: 70–85%\nAverage: 50–70%\nPoor: < 50% (sleep less than 4.5h)")
                .setNegativeButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

   /*private void showAlarmCountdownNotification(long millisUntilAlarm) {
        long totalMinutes = millisUntilAlarm / (60 * 1000);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        String timeText = (hours > 0) ? (hours + "h " + minutes + "min") : (minutes + " min");
        String content = "Your smart alarm is set in " + timeText;


        NotificationManager notificationManager = (NotificationManager)
                getContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sleep_alarm_channel",
                    "Sleep Alarm Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), "sleep_alarm_channel")
                .setSmallIcon(R.drawable.clock) // replace with your icon
                .setContentTitle("⏰ Smart Alarm Scheduled")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(101, builder.build());
    }*/


    //Avg Function
    private void avg_sleep() {
        FirebaseFirestore.getInstance().collection("Users")
                .document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fetch = documentSnapshot.getString("7days");
                    if (fetch == null) return;

                    String[] sp = fetch.split(";");
                    float totalScore = 0f;

                    for (int i = 0; i < 7; i++) {
                        float percent = Float.parseFloat(sp[i]);
                        float hours = (percent * 24f) / 100f;

                        float dayScore;
                        if (hours >= 7.5f && hours <= 9f) {
                            dayScore = 100f; // ideal sleep
                        } else if (hours < 7.5f) {
                            dayScore = (hours / 7.5f) * 100f;
                        } else { // >9h, penalize oversleeping
                            dayScore = (9f / hours) * 100f;
                        }

                        totalScore += dayScore;
                    }

                    float sleepScore = totalScore / 7f;
                    if (sleepScore > 100f) sleepScore = 100f;

                    float avgHours = (sleepScore * 8f) / 100f; // 8h as base for visual
                    int H = (int) avgHours;
                    int M = Math.round((avgHours - H) * 60);

                    String reaction;
                    if (sleepScore < 50) {
                        reaction = "😴";
                    } else if (sleepScore < 70) {
                        reaction = "😐";
                    } else if (sleepScore < 85) {
                        reaction = "😊";
                    } else {
                        reaction = "😁";
                    }

                    binding.sleepScoreDataEmoji.setText(reaction);
                    binding.avgSleepDurationHrs.setText(H + "hr " + M + "min");
                    binding.sleepScoreData.setText(String.format("%.1f %%", sleepScore));
                });
    }



    private void updateRecommendation() {
        FirebaseFirestore.getInstance().collection("Users")
                .document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fetch = documentSnapshot.getString("7days");
                    if (fetch == null) return;

                    String[] sp = fetch.split(";");
                    float sumPercentage = 0;
                    for (int i = 0; i < 7; i++) {
                        sumPercentage += Float.parseFloat(sp[i]);
                    }

                    float avgPercentage = sumPercentage / 7f;
                    float avgHours = (avgPercentage * 24f) / 100f;

                    String recommendation;
                    if (avgHours < 5) {
                        recommendation = "Too little sleep. Try aiming for 7–9 hours daily.";
                    } else if (avgHours < 7) {
                        recommendation = "Getting closer. Try to improve sleep consistency.";
                    } else if (avgHours <= 9) {
                        recommendation = "Perfect! You're in the ideal sleep range.";
                    } else if (avgHours <= 11) {
                        recommendation = "Slightly oversleeping. Maintain a balanced routine.";
                    } else {
                        recommendation = "Too much sleep. Monitor your health if this persists.";
                    }

                    binding.tvRecommendation.setText(recommendation);
                })
                .addOnFailureListener(e -> binding.tvRecommendation.setText("Unable to load recommendation."));
    }


    private void showProgressDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(progressDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

    }


    private void toggleDoNotDisturb(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // Ask user for permission
                Toast.makeText(getContext(), "Please enable DND permission", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                return;
            }


            if (enable) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }

            notificationManager.setInterruptionFilter(
                    enable ? NotificationManager.INTERRUPTION_FILTER_NONE : NotificationManager.INTERRUPTION_FILTER_ALL
            );

            Log.d("SleepApp", "DND set to " + (enable ? "ON" : "OFF"));
            Toast.makeText(getContext(), "DND " + (enable ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();

        }
    }

    private void showPreSleepHabitDialog(Runnable onComplete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_habits, null);
        builder.setView(dialogView);

        RadioGroup caffeineGroup = dialogView.findViewById(R.id.caffeineGroup);
        RadioGroup stressGroup = dialogView.findViewById(R.id.stressGroup);
        RadioGroup exerciseGroup = dialogView.findViewById(R.id.exerciseGroup);

        builder.setTitle("Today's Habits")
                .setPositiveButton("Save & Sleep", (dialog, id) -> {
                    String caffeine = getSelectedOption(caffeineGroup);
                    String stress = getSelectedOption(stressGroup);
                    String exercise = getSelectedOption(exerciseGroup);

                    String todayDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    Map<String, Object> habits = new HashMap<>();
                    habits.put("caffeine", caffeine);
                    habits.put("stress", stress);
                    habits.put("exercise", exercise);

                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(userID)
                            .collection("SleepRecords")
                            .document(todayDate)
                            .set(habits, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Habits saved!", Toast.LENGTH_SHORT).show());


                    int baseCycles = 5;
                    if (!caffeine.equalsIgnoreCase("None")) baseCycles++;
                    if (stress.equalsIgnoreCase("High")) baseCycles++;
                    if (exercise.equalsIgnoreCase("Yes") && caffeine.equalsIgnoreCase("None") && !stress.equalsIgnoreCase("High")) baseCycles--;
                    if (baseCycles < 4) baseCycles = 4;
                    if (baseCycles > 6) baseCycles = 6;


                    Intent intent = new Intent(getContext(), AlarmReceiver.class);

                    SharedPreferences prefs = getContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
                    String ringtoneUriStr = prefs.getString("ringtone_uri", null);
                    if (ringtoneUriStr != null) {
                        intent.putExtra("ringtoneUri", Uri.parse(ringtoneUriStr));
                    }
                    boolean isSmartAlarmEnabled = prefs.getBoolean("smart_alarm_enabled", true);

                    if (isSmartAlarmEnabled) {
                        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (!alarmManager.canScheduleExactAlarms()) {
                                Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(settingsIntent);
                                Toast.makeText(getContext(), "Autorise l’alarme exacte pour qu’elle fonctionne", Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        Calendar calendar = Calendar.getInstance();

                        calendar.add(Calendar.MINUTE, baseCycles * 90);
                        //calendar.add(Calendar.MINUTE, 1);

                        intent.setClass(getContext(), AlarmReceiver.class);
                        String ringtoneUriStrSmart = prefs.getString("ringtone_uri", null);
                        if (ringtoneUriStr != null) {
                            intent.putExtra("ringtoneUri", Uri.parse(ringtoneUriStr));
                        }

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                getContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                        startCountdownNotification(calendar.getTimeInMillis());
                    }


                    Map<String, Object> cycleMap = new HashMap<>();
                    cycleMap.put("cyclesUsed", baseCycles);
                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(userID)
                            .collection("SleepRecords")
                            .document(todayDate)
                            .set(cycleMap, SetOptions.merge());

                    onComplete.run();
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

        builder.create().show();
    }

    private String getSelectedOption(RadioGroup group) {
        int selectedId = group.getCheckedRadioButtonId();
        RadioButton button = group.findViewById(selectedId);
        return (button != null) ? button.getText().toString() : "Not answered";
    }

    private void showAlarmCountdownNotification(long millisLeft) {
        String message;

        if (millisLeft >= 3600000) { // >= 1 hour
            long hours = millisLeft / (1000 * 60 * 60);
            long minutes = (millisLeft / (1000 * 60)) % 60;
            message = hours + "h " + minutes + "min";
        } else if (millisLeft >= 60000) { // >= 1 min
            long minutes = millisLeft / (1000 * 60);
            message = minutes + " min";
        } else {
            long seconds = millisLeft / 1000;
            message = seconds + " sec";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "alarm_channel")
                .setSmallIcon(R.drawable.clock)
                .setContentTitle("Smart Alarm")
                .setContentText("Réveil dans " + message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(false)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }

        NotificationManagerCompat manager = NotificationManagerCompat.from(requireContext());
        manager.notify(NOTIF_ID, builder.build());
    }


    private void startCountdownNotification(long targetTimeMillis) {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMillis = targetTimeMillis - System.currentTimeMillis();
                if (remainingMillis > 0) {
                    showAlarmCountdownNotification(remainingMillis);

                    // Mise à jour + rapide si < 1 minute
                    long delay = remainingMillis < 60000 ? 1000 : 60000;
                    countdownHandler.postDelayed(this, delay);
                } else {
                    cancelCountdownNotification();
                }
            }
        };
        countdownHandler.post(countdownRunnable);
    }


    private void cancelCountdownNotification() {
        if (!isAdded()) return;
        NotificationManagerCompat manager = NotificationManagerCompat.from(requireContext());
        manager.cancel(NOTIF_ID);
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (isAlarmReceiverRegistered) {
            try {
                requireContext().unregisterReceiver(alarmTriggeredReceiver);
                isAlarmReceiverRegistered = false;
            } catch (IllegalArgumentException e) {
                Log.w("HomeFragment", "Receiver already unregistered");
            }
        }
    }




}