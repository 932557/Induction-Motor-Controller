package com.example.induction_motor_controller;

//login user daily data with high temperature and date
public class DailyLogPojo {

    String date;
    int high_temp;

    public DailyLogPojo(String date, int high_temp) {
        this.high_temp = high_temp;
        this.date = date;
    }

    public DailyLogPojo() {}

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getHigh_temp() {
        return high_temp;
    }

    public void setHigh_temp(int high_temp) {
        this.high_temp = high_temp;
    }
}

