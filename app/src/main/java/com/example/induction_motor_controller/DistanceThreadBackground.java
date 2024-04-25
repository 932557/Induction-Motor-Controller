package com.example.induction_motor_controller;

import static com.example.induction_motor_controller.MainActivity.minimum_speed;
import static com.example.induction_motor_controller.MainActivity.maximum_speed;
import static com.example.induction_motor_controller.MainActivity.motorSpeed;

import android.util.Log;

public class DistanceThreadBackground extends Thread {

    long startTime = System.currentTimeMillis();
    private volatile boolean running = true;
    public static double distMeter = 0;


    @Override
    public void run() {
        running = true;
        while (running) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - startTime;
            long seconds = elapsedTime / 1000;
            int rpm = getRealRPM(motorSpeed);

            double distInches = rpm * Math.PI * 2.75591 /* wheel size in inches */ * ((double) seconds / 60);
            distMeter = Math.abs((distInches / 39.37));

            try {
                Thread.sleep(1000); // Sleep for 1 second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopThread() {
        running = false;
    }

    //    other methods
    private int getRealRPM(int currentValue) {
        int originalMin = minimum_speed;
        int originalMax = maximum_speed;

        int newMin = 0;
        int newMax = 30;

        int mappedValue = (int) (((float) (currentValue - originalMin) / (originalMax - originalMin)) * (newMax - newMin) + newMin);
        return mappedValue;
    }
}
