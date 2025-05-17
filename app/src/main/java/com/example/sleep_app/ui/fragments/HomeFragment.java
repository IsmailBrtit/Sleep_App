package com.example.sleep_app.ui.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
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


import com.example.sleep_app.receiver.AlarmReceiver;
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

    int sleepHour = -1, sleepMinute = -1;
    int wakeHour = -1, wakeMinute = -1;

    public HomeFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home,container,false);
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


                    String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    String sleepAt = sharedPreferences.getString("sleepAt", "");

                    Timestamp date_1 = stringToTimestamp(sharedPreferences.getString("age",""));
                    Timestamp date_2 = stringToTimestamp(timeStamp);
                    long milliseconds = date_2.getTime() - date_1.getTime();

                    String today_date = new SimpleDateFormat("dd-M-yyyy").format(new Date());
                    String todayDateFirestore = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

                    upDateWinNode(milliseconds,today_date);

                    long hours = (milliseconds / 1000) / 3600;
                    long minutes = ((milliseconds / 1000) / 60) % 60;
                    String duration = hours + "h " + minutes + "min";

                    Map<String, Object> recordMap = new HashMap<>();
                    recordMap.put("sleepAt", sleepAt);
                    recordMap.put("wakeUp", timeStamp);
                    recordMap.put("duration", duration);


                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(userID)
                            .collection("SleepRecords")
                            .document(todayDateFirestore)
                            .set(recordMap, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                // Optional: Toast success
                            })
                            .addOnFailureListener(e -> {
                                // Optional: Toast error
                            });

// ✅ Now clear shared prefs
                    SharedPreferences.Editor myEdit = sharedPreferences.edit();
                    myEdit.putString("name", null);
                    myEdit.putString("age", null);
                    myEdit.putString("wakeUp", timeStamp);
                    myEdit.commit();

                    setWakeTime();
                    countSec = false;
                    toggleDoNotDisturb(false);// DISABLE DND when ending sleep
                    getOperationLocally();
                }
            }
        });


        return view;
    }

    private void setWakeTime() {
        try{
            String wake = sharedPreferences.getString("wakeUp","");
            Timestamp timecurr = stringToTimestamp(wake);

            long hour = Objects.requireNonNull(timecurr).getHours();
            long min = timecurr.getMinutes();
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

                            String today_datee = new SimpleDateFormat("dd-M-yyyy").format(new Date());
                            long totalMillisec = (Math.abs(sleepHour - wakeHour) * 60
                                    + Math.abs(sleepMinute - wakeMinute)) * 60 * 1000;
                            upDateWinNode(totalMillisec,today_datee);

                            // 🔔 Planifie l'alarme
                            Calendar calendar = Calendar.getInstance();
                            calendar.set(Calendar.HOUR_OF_DAY, wakeHour);
                            calendar.set(Calendar.MINUTE, wakeMinute);
                            calendar.set(Calendar.SECOND, 0);
                            calendar.set(Calendar.MILLISECOND, 0);

                            Toast.makeText(getContext(), "Done. Alarm scheduled.", Toast.LENGTH_SHORT).show();

                            // Si l'heure est passée, programme pour demain
                            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                                calendar.add(Calendar.DAY_OF_MONTH, 1);
                            }

                            Intent intent = new Intent(getContext(), AlarmReceiver.class);

                            // 🔊 Récupère la sonnerie personnalisée si elle existe
                            SharedPreferences prefs = getContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
                            String ringtoneUriStr = prefs.getString("ringtone_uri", null);
                            if (ringtoneUriStr != null) {
                                intent.putExtra("ringtoneUri", Uri.parse(ringtoneUriStr));
                            }
                            int uniqueId = (int) System.currentTimeMillis();
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    getContext(), uniqueId, intent, PendingIntent.FLAG_IMMUTABLE);


                            AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                            alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                            long millisUntilAlarm = calendar.getTimeInMillis() - System.currentTimeMillis();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(getActivity(),
                                            new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                                    return; // stop if not granted
                                }
                            }

                            Log.d("SleepApp", "Calling notification with " + millisUntilAlarm + " ms");
                            Toast.makeText(getContext(), "Alarm in " + millisUntilAlarm / 60000 + " min", Toast.LENGTH_SHORT).show();

                            Log.e("MissClick", "Going to show countdown notification");

                            showAlarmCountdownNotification(millisUntilAlarm);
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
                                //sleepHourT = sleepHourT;
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
                               // wakeHour = wakeHour;

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
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date parsedDate = dateFormat.parse(date);
            return new Timestamp(parsedDate.getTime());
        } catch (Exception e) {
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

    private void showAlarmCountdownNotification(long millisUntilAlarm) {
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
    }


    //Avg Function
    private void avg_sleep() {
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

                    float sleepScore = sumPercentage / 7f;
                    if (sleepScore > 100f) sleepScore = 100f;

                    // Convert back to hours just for display
                    float avgHours = (sleepScore * 24f) / 100f;

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

                    int H = (int) avgHours;
                    int M = Math.round((avgHours - H) * 60);
                    String avgSleep = H + "hr " + M + "min";

                    binding.sleepScoreDataEmoji.setText(reaction);
                    binding.avgSleepDurationHrs.setText(avgSleep);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        countSec = false;
    }

    private void toggleDoNotDisturb(boolean enable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);

            if (!notificationManager.isNotificationPolicyAccessGranted()) {
                // Ask user for permission
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
                return;
            }

            if (enable) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
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

                    //  REVEIL INTELLIGENT

                    int baseCycles = 5;

                    if (caffeine.equalsIgnoreCase("Yes")) baseCycles++;
                    if (stress.equalsIgnoreCase("High")) baseCycles++;
                    if (exercise.equalsIgnoreCase("Yes") && !caffeine.equalsIgnoreCase("Yes") && !stress.equalsIgnoreCase("High")) baseCycles--;

                    if (baseCycles < 4) baseCycles = 4;
                    if (baseCycles > 6) baseCycles = 6;

                    SharedPreferences prefs = getContext().getSharedPreferences("SleepPrefs", Context.MODE_PRIVATE);
                    boolean isSmartAlarmEnabled = prefs.getBoolean("smart_alarm_enabled", true);
                    //  alarme apres N cycles
                    if (isSmartAlarmEnabled) {
                        // ⏰ 4. Programmer l’alarme après N * 90 minutes
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, baseCycles * 90);

                        Intent intent = new Intent(getContext(), AlarmReceiver.class);
                        String ringtoneUriStr = prefs.getString("ringtone_uri", null);
                        if (ringtoneUriStr != null) {
                            intent.putExtra("ringtoneUri", Uri.parse(ringtoneUriStr));
                        }

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                getContext(), 1, intent, PendingIntent.FLAG_IMMUTABLE);

                        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }

                    // 📊 5. Sauvegarder le nombre de cycles même si réveil désactivé
                    Map<String, Object> cycleMap = new HashMap<>();
                    cycleMap.put("cyclesUsed", baseCycles);

                    FirebaseFirestore.getInstance()
                            .collection("Users")
                            .document(userID)
                            .collection("SleepRecords")
                            .document(todayDate)
                            .set(cycleMap, SetOptions.merge());

                    // 🚀 Continuer la logique du bouton "Start Sleep"
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

}