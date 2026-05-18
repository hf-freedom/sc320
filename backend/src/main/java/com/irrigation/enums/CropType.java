package com.irrigation.enums;

public enum CropType {
    RICE("水稻", 0.8),
    WHEAT("小麦", 0.5),
    CORN("玉米", 0.6),
    COTTON("棉花", 0.4),
    SOYBEAN("大豆", 0.45),
    VEGETABLE("蔬菜", 0.7),
    FRUIT("果树", 0.55);

    private String name;
    private double waterCoefficient;

    CropType(String name, double waterCoefficient) {
        this.name = name;
        this.waterCoefficient = waterCoefficient;
    }

    public String getName() {
        return name;
    }

    public double getWaterCoefficient() {
        return waterCoefficient;
    }
}
