package com.irrigation.enums;

public enum DeviceStatus {
    RUNNING("运行中"),
    IDLE("空闲"),
    FAULT("故障"),
    MAINTENANCE("维护中");

    private String name;

    DeviceStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
