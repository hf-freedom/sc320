package com.irrigation.entity;

import lombok.Data;

@Data
public class DailyReport {
    private String date;
    private int totalFields;
    private int highRiskFields;
    private int mediumRiskFields;
    private int lowRiskFields;
    private double deviceLoadRate;
    private int runningDevices;
    private int totalDevices;
    private double waterRemaining;
    private double waterUsageToday;
    private String generatedTime;
}
