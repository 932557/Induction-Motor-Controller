package com.example.induction_motor_controller;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    int minimum_speed = 200, maximum_speed = 1024;

    ValueEventListener motor_speed_event_listener, motor_event_on_listener,
            user_name_event_listener, motor_name_event_listener, daily_temp_event_listener;


    LineChart tempLineChart;
    TextView userNameTxt, machineNameTxt, machineStatusTxt, machineSpeedTxt, noDataFoundTxt;

    FirebaseDatabase db;
    DatabaseReference dbRef;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);
        initViews();
        initDatabase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        refuseEventListeners();
    }

//    removing event listener when this screen gets closed
    private void refuseEventListeners() {
        dbRef.removeEventListener(motor_speed_event_listener);
        dbRef.removeEventListener(motor_event_on_listener);
        dbRef.removeEventListener(user_name_event_listener);
        dbRef.removeEventListener(motor_name_event_listener);
        dbRef.removeEventListener(daily_temp_event_listener);
    }

    private void initViews() {
        tempLineChart = findViewById(R.id.chart);
        userNameTxt = findViewById(R.id.tv_username);
        machineNameTxt = findViewById(R.id.tv_machine_name);
        machineStatusTxt = findViewById(R.id.tv_machine_status);
        machineSpeedTxt = findViewById(R.id.tv_machine_speed);
        noDataFoundTxt = findViewById(R.id.noDataMessage);
    }

//    connecting to database
    private void initDatabase() {
        db = FirebaseDatabase.getInstance();
        dbRef = db.getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
        }
    }

//    getting data from database to show history and data of machine and user
    private void setupValues() {
        motor_event_on_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int isMachineOn = snapshot.getValue(Integer.class);
                String str_txt;
                int txt_color;
                if (isMachineOn == 1) {
                    str_txt = "Running";
                    txt_color = Color.parseColor("#00ff00");
                } else {
                    str_txt = "Stopped";
                    txt_color = Color.parseColor("#ff0000");
                }
                machineStatusTxt.setText(str_txt);
                machineStatusTxt.setTextColor(txt_color);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String str_txt = "Stopped";
                int txt_color = Color.parseColor("#ff0000");
                machineStatusTxt.setText(str_txt);
                machineStatusTxt.setTextColor(txt_color);
            }
        };

        motor_speed_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int motorSpeed = snapshot.getValue(Integer.class);
                float percentage = ((float) (motorSpeed - minimum_speed) / (maximum_speed - minimum_speed)) * 100;
                percentage = Math.max(0, Math.min(100, percentage));
                String speed = ((int)percentage) + " %";
                machineSpeedTxt.setText(speed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String speed = (0) + " %";
                machineSpeedTxt.setText(speed);
            }
        };

        user_name_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value == null)
                    value = "None";
                userNameTxt.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                String value = "None";
                userNameTxt.setText(value);
            }
        };

        motor_name_event_listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                if (value == null)
                    value = "None";
                machineNameTxt.setText(value);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                String value = "None";
                machineNameTxt.setText(value);
            }
        };


        daily_temp_event_listener = new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserPojo userPojo = snapshot.getValue(UserPojo.class);
                if (userPojo!= null) {
                    List<DailyLogPojo> dailyLogTemp = userPojo.getDailyLog();
                    if (dailyLogTemp != null) {
                        if (dailyLogTemp.size() > 0) {
                            dataFoundExtractData(dailyLogTemp);
                        } else {
                            noDataFound();
                        }
                    } else {
                        noDataFound();
                    }
                } else {
                    noDataFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this, "Canceled", Toast.LENGTH_SHORT).show();
            }
        };




//        initializing event listeners
        dbRef.child("/motor").addValueEventListener(motor_event_on_listener);

        dbRef.child("/motors").addValueEventListener(motor_speed_event_listener);

        dbRef.child("users/" + currentUser.getUid() + "/userName").addValueEventListener(user_name_event_listener);

        dbRef.child("users/" + currentUser.getUid() + "/motorName").addValueEventListener(motor_name_event_listener);

        dbRef.child("users/" + currentUser.getUid()).addValueEventListener(daily_temp_event_listener);
    }


//    if no history data found then don't show graph show text of 'no data found'
    private void noDataFound() {
        tempLineChart.setVisibility(View.GONE);
        noDataFoundTxt.setVisibility(View.VISIBLE);
    }

//    if history data found then don't show text of 'no data found' show graph and extracting data from user
    private void dataFoundExtractData(List<DailyLogPojo> dailyLogPojos) {
        noDataFoundTxt.setVisibility(View.GONE);
        tempLineChart.setVisibility(View.VISIBLE);

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<String> datesList = new ArrayList<>();
        List<Long> tempList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (DailyLogPojo dailyLog: dailyLogPojos) {
            tempList.add((long) dailyLog.getHigh_temp());
            String date = dailyLog.getDate();
            try {
                Date d = dateFormat.parse(date);
                if (d != null) {
                    String dateStr = d.getDate() + "/" + months[d.getMonth()];
                    datesList.add(dateStr);
                } else {
                    datesList.add("N/A");
                }
            } catch (Exception e) {
                datesList.add("N/A");
            }
        }
        Collections.reverse(datesList);
        Collections.reverse(tempList);

        setupLineChartTemp();
        addDataToChartTemp(datesList, tempList);
    }


//    setting graph chart
    private void setupLineChartTemp() {
        int colorTheme = MaterialColors.getColor(UserDashboardActivity.this,
                com.google.android.material.R.attr.colorOnPrimary, Color.parseColor("#9f9f9f"));

        // Customize line chart appearance
        tempLineChart.setDrawGridBackground(false);
        tempLineChart.getDescription().setEnabled(false);
        tempLineChart.setTouchEnabled(false);
        tempLineChart.setDragEnabled(false);
        tempLineChart.setScaleEnabled(false);
        tempLineChart.setPinchZoom(false);
        tempLineChart.setDoubleTapToZoomEnabled(false);

        XAxis xAxis = tempLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = tempLineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(colorTheme); // Set left Y-axis text color to white

        // Disable right Y-axis
        YAxis rightAxis = tempLineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    private void addDataToChartTemp(List<String> dates, List<Long> labels) {
        int colorTheme = MaterialColors.getColor(UserDashboardActivity.this,
                com.google.android.material.R.attr.colorOnPrimary, Color.parseColor("#9f9f9f"));

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < dates.size(); i++) {
            if (labels != null && labels.size() > i) {
                entries.add(new Entry(i, labels.get(i)));
            } else {
                entries.add(new Entry(i, 0));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "High Temperature");
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);

        // Set text color to white for all components
        dataSet.setValueTextColor(Color.WHITE);

        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);

        LineData lineData = new LineData(dataSets);
        XAxis xAxis = tempLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(45);
        xAxis.setGranularity(1f);

        tempLineChart.setData(lineData);

        // Set text color to white for all components
        tempLineChart.getLegend().setTextColor(colorTheme);
        xAxis.setTextColor(colorTheme);
        tempLineChart.invalidate();
    }


}