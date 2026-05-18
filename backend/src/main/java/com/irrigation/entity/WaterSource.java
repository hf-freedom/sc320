package com.irrigation.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaterSource {
    private String id;
    private String name;
    private double totalCapacity;
    private double remainingCapacity;
    private double dailySupplyLimit;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
