package com.irrigation.entity;

import com.irrigation.enums.IrrigationPlanStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IrrigationPlan {
    private String id;
    private String fieldId;
    private String deviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double waterAmount;
    private int priority;
    private IrrigationPlanStatus status;
    private String reason;
    private boolean manualModified;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
