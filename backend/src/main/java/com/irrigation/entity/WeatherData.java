package com.irrigation.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WeatherData {
    private LocalDate date;
    private double temperature;
    private double humidity;
    private double rainfall;
    private String weatherCondition;
    private int consecutiveHighTempDays;
}
