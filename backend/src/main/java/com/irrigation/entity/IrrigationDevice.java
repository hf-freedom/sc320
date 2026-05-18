package com.irrigation.entity;

import com.irrigation.enums.DeviceStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IrrigationDevice {
    private String id;
    private String name;
    private double capacity;
    private DeviceStatus status;
    private String fieldId;
    private LocalDateTime lastMaintenanceTime;
    private String faultMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
