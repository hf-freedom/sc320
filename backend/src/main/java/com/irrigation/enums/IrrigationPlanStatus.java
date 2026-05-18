package com.irrigation.enums;

public enum IrrigationPlanStatus {
    PENDING("待执行"),
    IN_PROGRESS("执行中"),
    COMPLETED("已完成"),
    PAUSED("已暂停"),
    CANCELLED("已取消");

    private String name;

    IrrigationPlanStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
