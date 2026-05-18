package com.irrigation.entity;

import com.irrigation.enums.CropType;
import com.irrigation.enums.GrowthStage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Field {
    private String id;
    private String name;
    private double area;
    private CropType cropType;
    private GrowthStage growthStage;
    private double soilMoisture;
    private double cropValue;
    private int level;
    private String location;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
