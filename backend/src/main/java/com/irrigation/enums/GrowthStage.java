package com.irrigation.enums;

public enum GrowthStage {
    SEEDLING("苗期", 0.3),
    VEGETATIVE("营养生长期", 0.6),
    FLOWERING("开花期", 0.9),
    FRUITING("结果期", 0.8),
    RIPENING("成熟期", 0.4);

    private String name;
    private double waterThreshold;

    GrowthStage(String name, double waterThreshold) {
        this.name = name;
        this.waterThreshold = waterThreshold;
    }

    public String getName() {
        return name;
    }

    public double getWaterThreshold() {
        return waterThreshold;
    }
}
