package com.irrigation.config;

import com.irrigation.entity.*;
import com.irrigation.enums.CropType;
import com.irrigation.enums.DeviceStatus;
import com.irrigation.enums.GrowthStage;
import com.irrigation.enums.IrrigationPlanStatus;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DataStorage dataStorage;

    @Override
    public void run(String... args) {
        initFields();
        initDevices();
        initWaterSources();
        initWeatherData();
        System.out.println("初始化数据完成！");
    }

    private void initFields() {
        String[] fieldNames = {"东一号地块", "西二号地块", "南三号地块", "北四号地块", "中五号地块"};
        CropType[] cropTypes = CropType.values();
        GrowthStage[] stages = GrowthStage.values();
        double[] areas = {5.2, 3.8, 6.5, 4.2, 7.0};
        double[] moistures = {0.45, 0.32, 0.58, 0.28, 0.50};
        double[] values = {5000, 3500, 8000, 4200, 6500};
        int[] levels = {1, 2, 1, 3, 2};

        for (int i = 0; i < fieldNames.length; i++) {
            Field field = new Field();
            field.setId("FIELD_" + (i + 1));
            field.setName(fieldNames[i]);
            field.setArea(areas[i]);
            field.setCropType(cropTypes[i % cropTypes.length]);
            field.setGrowthStage(stages[i % stages.length]);
            field.setSoilMoisture(moistures[i]);
            field.setCropValue(values[i]);
            field.setLevel(levels[i]);
            field.setLocation("位置" + (i + 1));
            field.setCreateTime(LocalDateTime.now());
            field.setUpdateTime(LocalDateTime.now());
            dataStorage.getFields().put(field.getId(), field);
        }
    }

    private void initDevices() {
        String[] deviceNames = {"灌溉设备A", "灌溉设备B", "灌溉设备C", "灌溉设备D", "灌溉设备E"};
        double[] capacities = {10.0, 8.0, 12.0, 9.0, 11.0};

        for (int i = 0; i < deviceNames.length; i++) {
            IrrigationDevice device = new IrrigationDevice();
            device.setId("DEVICE_" + (i + 1));
            device.setName(deviceNames[i]);
            device.setCapacity(capacities[i]);
            device.setStatus(DeviceStatus.IDLE);
            device.setFieldId(null);
            device.setLastMaintenanceTime(LocalDateTime.now().minusDays(7));
            device.setCreateTime(LocalDateTime.now());
            device.setUpdateTime(LocalDateTime.now());
            dataStorage.getDevices().put(device.getId(), device);
        }
    }

    private void initWaterSources() {
        WaterSource ws = new WaterSource();
        ws.setId("WS_1");
        ws.setName("主水库");
        ws.setTotalCapacity(1000.0);
        ws.setRemainingCapacity(850.0);
        ws.setDailySupplyLimit(100.0);
        ws.setCreateTime(LocalDateTime.now());
        ws.setUpdateTime(LocalDateTime.now());
        dataStorage.getWaterSources().put(ws.getId(), ws);
    }

    private void initWeatherData() {
        LocalDate today = LocalDate.now();
        WeatherData weather = new WeatherData();
        weather.setDate(today);
        weather.setTemperature(28.5);
        weather.setHumidity(65);
        weather.setRainfall(0);
        weather.setWeatherCondition("晴天");
        weather.setConsecutiveHighTempDays(0);
        dataStorage.getWeatherDataMap().put(today.toString(), weather);
    }
}
