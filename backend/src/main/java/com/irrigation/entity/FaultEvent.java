package com.irrigation.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FaultEvent {
    private String id;
    private String deviceId;
    private String deviceName;
    private String faultMessage;
    private String affectedFieldId;
    private LocalDateTime faultTime;
    private LocalDateTime resolvedTime;
    private boolean resolved;
    private int pausedPlansCount;
    private int reallocatedPlansCount;
    private List<String> reallocatedPlanIds;
    private String newDeviceId;
    private String newDeviceName;
    private String remarks;
}
