package com.example.induction_motor_controller;


import static com.example.induction_motor_controller.DistanceThreadBackground.distMeter;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS = 129;
    int isMachineOn = 0;
    int machineClockVise = 0;
    public static int motorSpeed = 0;
    int motorTemp = 0;
    int motorExceedTemp = 0;
    int motorVibr = 0;

    public static int minimum_speed = 200, maximum_speed = 1024;

    int normal_color, gray_color;
    boolean notificationAllowed = false;

    FirebaseDatabase db;
    DatabaseReference dbRef;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    TextView motor_is_on_txt, machine_temp_txt, clock_anti_clock_txt, motor_speed_txt, machine_speed_txt_head,
            machine_temp_txt_head, vibration_status_txt_head, vibration_status_txt,
            total_distance_travelled_txt_head, total_distance_travelled_txt, machine_name_txt, user_name_txt;
    SwitchMaterial clock_anti_clock_switch;
    MaterialButton turn_on_motor_btn;
    AppCompatSeekBar motor_speed_seek;
    MaterialCardView profile_card;


    //    listeners
    ValueEventListener temp_event_listener, motor_speed_event_listener, motor_event_on_listener,
            clock_anti_clock_listener, vibr_event_listener, user_name_event_listener, motor_name_event_listener;

    DistanceThreadBackground distanceThreadBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to RegisterActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        } else {
//            user already logged in
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS);
                } else {
                    notificationAllowed = true;
                }
            }

//            notification code start
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("my_channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("This is my channel");
                notificationManager.createNotificationChannel(channel);
            }
//            notification code end

            findViews();
            connectDB();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                notificationAllowed = true;
            } else {
                // Permission denied
                notificationAllowed = false;
                Toast.makeText(this, "Permission denied to show notifications", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    machine is off so reduce colors of all the machine details text
    private void machineIsOff() {
        machine_speed_txt_head.setAlpha(0.6f);
        machine_temp_txt_head.setAlpha(0.6f);
        clock_anti_clock_txt.setAlpha(0.6f);
        motor_speed_txt.setAlpha(0.6f);
        machine_temp_txt.setAlpha(0.6f);
        total_distance_travelled_txt.setAlpha(0.6f);
        total_distance_travelled_txt_head.setAlpha(0.6f);
        vibration_status_txt.setAlpha(0.6f);
        vibration_status_txt_head.setAlpha(0.6f);
        clock_anti_clock_switch.setAlpha(0.6f);
        motor_speed_seek.setAlpha(0.6f);
        clock_anti_clock_switch.setClickable(false);
        motor_speed_seek.setEnabled(false);
    }

//    machine is on so re bright colors of all the machine details text
    private void machineIsOn() {
        machine_speed_txt_head.setAlpha(1f);
        machine_temp_txt_head.setAlpha(1f);
        clock_anti_clock_txt.setAlpha(1f);
        motor_speed_txt.setAlpha(1f);
        machine_temp_txt.setAlpha(1f);
        clock_anti_clock_switch.setAlpha(1f);
        motor_speed_seek.setAlpha(1f);
        total_distance_travelled_txt.setAlpha(1f);
        total_distance_travelled_txt_head.setAlpha(1f);
        vibration_status_txt.setAlpha(1f);
        vibration_status_txt_head.setAlpha(1f);
        clock_anti_clock_switch.setClickable(true);
        motor_speed_seek.setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getActualValues();
        performActions();
        calculateTime((isMachineOn == 1));
    }

    @Override
    protected void onPause() {
        super.onPause();
        refuseEventListeners();
    }

//    if temperature gets high than actual temperature limit then it shows notification
    private void tempNotification(int temperature) {
        String celcius = (char) 0x00B0+"c";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id")
                .setSmallIcon(R.mipmap.ic_alert)
                .setContentTitle("High Temperature")
                .setContentText("Temperature Exceeded " + temperature + celcius + " turn off the motor");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
    }

//    if vibration found in machine then notify
    private void vibrationNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id")
                .setSmallIcon(R.mipmap.ic_alert)
                .setContentTitle("Vibration Found")
                .setContentText("Vibration found in the motor");
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(2, builder.build());
    }

//    when app is closed then revoke all listening ability
    private void refuseEventListeners() {
        dbRef.removeEventListener(temp_event_listener);
        dbRef.removeEventListener(motor_speed_event_listener);
        dbRef.removeEventListener(motor_event_on_listener);
        dbRef.removeEventListener(clock_anti_clock_listener);
        dbRef.removeEventListener(vibr_event_listener);
        dbRef.removeEventListener(user_name_event_listener);
        dbRef.removeEventListener(motor_name_event_listener);
    }

//    initializing views
    private void findViews() {
        motor_is_on_txt = MainActivity.this.findViewById(R.id.main_txt);
        machine_temp_txt = findViewById(R.id.machine_temp);
        clock_anti_clock_txt = findViewById(R.id.clock_anti_machine_text);
        motor_speed_txt = findViewById(R.id.machine_speed);
        clock_anti_clock_switch = findViewById(R.id.clock_anti_machine_switch);
        turn_on_motor_btn = findViewById(R.id.on_off_machine_button);
        motor_speed_seek = findViewById(R.id.machine_speed_bar);
        machine_speed_txt_head = findViewById(R.id.machine_speed_text);
        machine_temp_txt_head = findViewById(R.id.machine_temp_text);
        vibration_status_txt_head = findViewById(R.id.machine_vibr_text);
        vibration_status_txt = findViewById(R.id.machine_vibr);
        total_distance_travelled_txt_head = findViewById(R.id.machine_dist_travel_text);
        total_distance_travelled_txt = findViewById(R.id.machine_dist_travel);
        machine_name_txt = findViewById(R.id.machine_name_txt);
        user_name_txt = findViewById(R.id.user_name);
        profile_card = findViewById(R.id.profile_card);

        gray_color = Color.parseColor("#808080");
        normal_color = motor_is_on_txt.getCurrentTextColor();

//        created thread object at initial level
        distanceThreadBackground = new DistanceThreadBackground();
    }

//    database connection
    private void connectDB() {
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
    }

    Handler handler;
    Runnable runnable;
//    showing distance from machine start and saving to database and calculating time
    private void calculateTime(boolean isOn) {
        if (handler == null)
            handler = new Handler();
        if (isOn) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    total_distance_travelled_txt.setText(String.format("%.2f m", distMeter));
                    dbRef.child("dist").setValue(distMeter);
                    handler.postDelayed(this, 1000);
                }
            };
            handler.postDelayed(runnable, 0);
        } else {
            handler.removeCallbacks(runnable);
            runnable = null;
            total_distance_travelled_txt.setText("0");
            dbRef.child( "dist").setValue(0);
        }
    }

//    initializing listeners and assinging to it
    private void getActualValues() {

        clock_anti_clock_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                machineClockVise = snapshot.getValue(Integer.class);
                String clock_or_anti_str = "Machine Direction ";
                boolean isOn;
                if (machineClockVise == 0) {
                    clock_or_anti_str += "(ACW)";
                    isOn = true;
                } else {
                    clock_or_anti_str += "(CW)";
                    isOn = false;
                }
                clock_anti_clock_txt.setText(clock_or_anti_str);
                clock_anti_clock_switch.setChecked(isOn);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Found Error : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        motor_event_on_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isMachineOn = snapshot.getValue(Integer.class);
                String btn_txt;
                String str_txt;
                int txt_color;
                boolean isMotorOn;
                if (isMachineOn == 1) {
                    btn_txt = "Turn Off Motor";
                    str_txt = "Machine is On";
                    txt_color = Color.parseColor("#00ff00");
                    isMotorOn = true;
                    if (distanceThreadBackground != null && !distanceThreadBackground.isAlive())
                        distanceThreadBackground.start();
                } else {
                    btn_txt = "Turn On Motor";
                    str_txt = "Machine is Off";
                    txt_color = normal_color;
                    isMotorOn = false;
                    if (distanceThreadBackground != null) {
                        distanceThreadBackground.stopThread();
                        distanceThreadBackground = new DistanceThreadBackground();
                        distMeter = 0;
                    }
                }
                turn_on_motor_btn.setText(btn_txt);
                motor_is_on_txt.setText(str_txt);
                motor_is_on_txt.setTextColor(txt_color);
                if(isMotorOn) machineIsOn(); else machineIsOff();
                calculateTime(isMotorOn);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Found Error : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        motor_speed_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                motorSpeed = snapshot.getValue(Integer.class);
                float percentage = ((float) (motorSpeed - minimum_speed) / (maximum_speed - minimum_speed)) * 100;
                percentage = Math.max(0, Math.min(100, percentage));
                String speed = ((int)percentage) + " %";
                motor_speed_txt.setText(speed);
                motor_speed_seek.setProgress(motorSpeed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Found Error : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        temp_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                motorTemp = snapshot.getValue(Integer.class);
                dbRef.child("templ").get().addOnSuccessListener(dataSnapshot -> {
                    motorExceedTemp = dataSnapshot.getValue(Integer.class);
                    int colorTemp;
                    if (motorTemp >= motorExceedTemp) {
                        colorTemp = Color.parseColor("#ff0000");
                        tempNotification(motorTemp);
                    } else {
                        colorTemp = normal_color;
                    }
                    String celcius = (char) 0x00B0+"c";
                    String temp_str = motorTemp + celcius;
                    machine_temp_txt.setText(temp_str);
                    machine_temp_txt.setTextColor(colorTemp);

                    tempEventOccurred(motorTemp);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Found Error Temp : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        vibr_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                motorVibr = snapshot.getValue(Integer.class);
                String alertStr;
                int alertColor;
                if (motorVibr == 1) {
                    vibrationNotification();
                    alertStr = "Alert!";
                    alertColor = Color.parseColor("#ff0000");
                } else {
                    alertStr = "No Alert";
                    alertColor = Color.parseColor("#00ff00");
                }
                vibration_status_txt.setText(alertStr);
                vibration_status_txt.setTextColor(alertColor);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Found Error Vibration : " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        user_name_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value == null)
                    value = "None";
                user_name_txt.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors here
            }
        };

        motor_name_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value == null)
                    value = "None";
                machine_name_txt.setText(value + " : ");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle potential errors here
            }
        };



        dbRef.child("motord").addValueEventListener(clock_anti_clock_listener);

        dbRef.child("motor").addValueEventListener(motor_event_on_listener);

        dbRef.child("motors").addValueEventListener(motor_speed_event_listener);

        dbRef.child("Temp").addValueEventListener(temp_event_listener);

        dbRef.child("vsen").addValueEventListener(vibr_event_listener);

        dbRef.child("users/" + currentUser.getUid() + "/userName").addValueEventListener(user_name_event_listener);

        dbRef.child("users/" + currentUser.getUid() + "/motorName").addValueEventListener(motor_name_event_listener);

    }

//    saving daily high temperature to current logged in user
    private void tempEventOccurred(int motorTemp) {
        dbRef.child("users/" + currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Handle the data here

                UserPojo userPojo = dataSnapshot.getValue(UserPojo.class);
                if (userPojo != null) {
                    List<DailyLogPojo> dailyLogList = userPojo.getDailyLog();
                    DailyLogPojo dailyLog;

                    if (dailyLogList == null) {
                        dailyLogList = new ArrayList<>();
                    }

                    Date currentDate = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String formattedDate = dateFormat.format(currentDate);

                    if (dailyLogList.size() > 0) {
                        dailyLog = dailyLogList.get(dailyLogList.size() - 1);
                        if (formattedDate.equals(dailyLog.getDate())) {
                            dailyLog.setHigh_temp(Math.max(dailyLog.getHigh_temp(), motorTemp));
                            dailyLogList.set(dailyLogList.size() - 1, dailyLog);
                        } else {
//                    date not matched create new date
                            dailyLog = new DailyLogPojo(formattedDate, motorTemp);
                            dailyLogList.add(dailyLogList.size(), dailyLog);
                        }
                    } else {
                        dailyLog = new DailyLogPojo(formattedDate, motorTemp);
                        dailyLogList.add(dailyLog);
                    }

                    dbRef.child("users/" + currentUser.getUid() + "/dailyLog").setValue(dailyLogList);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

//    performing action's to every button clicks
    private void performActions() {
        
        profile_card.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.profile_menu, popupMenu.getMenu());

            // Handle menu item clicks
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_dashboard) {
                    Intent intent = new Intent(MainActivity.this, UserDashboardActivity.class);
                    startActivity(intent);
                    return true;
                } else if (itemId == R.id.menu_logout) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    View dialogView = getLayoutInflater().inflate(R.layout.logout_alert_dialog, null);
                    builder.setView(dialogView);

                    TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
                    TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
                    dialogTitle.setText("Confirm Logout");
                    dialogMessage.setText("Are you sure you want to log out?");

                    Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
                    Button btnLogout = dialogView.findViewById(R.id.btn_logout);

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                    btnCancel.setOnClickListener(v -> {
                        alertDialog.dismiss();
                    });

                    btnLogout.setOnClickListener(v -> {
                        logOutUser();
                        alertDialog.dismiss();
                    });

                    return true;
                } else {
                    return false;
                }
            });

            popupMenu.show();
        });
        
        turn_on_motor_btn.setOnClickListener(v -> {
            int newIsMachineOn = (isMachineOn == 0) ? 1 : 0;
            dbRef.child("motor").setValue(newIsMachineOn).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Failed to Turn on Machine", Toast.LENGTH_SHORT).show();
                }
            });
        });

        clock_anti_clock_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int isClock = (isChecked) ? 0 : 1 ;
            dbRef.child("motord").setValue(isClock).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Failed to On/Off Anticlockwise", Toast.LENGTH_SHORT).show();
                    clock_anti_clock_switch.setChecked((machineClockVise == 0));
                }
            });
        });

        motor_speed_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                dbRef.child( "motors").setValue(seekBar.getProgress()).addOnCompleteListener(task -> {
                    if (!task.isSuccessful())
                        Toast.makeText(MainActivity.this, "Failed to update Speed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


//    logout current user method
    private void logOutUser() {
        if (mAuth == null)
            mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();

//        finish this screen and start login screen
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

}