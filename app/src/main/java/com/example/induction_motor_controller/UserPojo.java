package com.example.induction_motor_controller;

import java.util.List;

//login user class
public class UserPojo {

    String userName;
    String emailId;
    String motorName;
    List<DailyLogPojo> dailyLog;


    public UserPojo(String userName, String emailId, String motorName, List<DailyLogPojo> dailyLog) {
        this.userName = userName;
        this.emailId = emailId;
        this.motorName = motorName;
        this.dailyLog = dailyLog;
    }

    public UserPojo() {}

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public String getMotorName() {
        return motorName;
    }

    public void setMotorName(String motorName) {
        this.motorName = motorName;
    }

    public List<DailyLogPojo> getDailyLog() {
        return dailyLog;
    }

    public void setDailyLog(List<DailyLogPojo> dailyLog) {
        this.dailyLog = dailyLog;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
