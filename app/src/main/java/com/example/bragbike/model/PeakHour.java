package com.example.bragbike.model;

import com.google.gson.annotations.SerializedName;

public class PeakHour {
    private int id;
    private String name;
    
    @SerializedName("start_time")
    private String startTime;
    
    @SerializedName("end_time")
    private String endTime;
    
    @SerializedName("days_of_week")
    private String daysOfWeek;
    
    @SerializedName("is_active")
    private boolean isActive;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(String daysOfWeek) { this.daysOfWeek = daysOfWeek; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
