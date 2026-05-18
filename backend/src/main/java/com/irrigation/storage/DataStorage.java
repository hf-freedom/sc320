package com.irrigation.storage;

import com.irrigation.entity.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataStorage {
    private final Map<String, Field> fields = new ConcurrentHashMap<>();
    private final Map<String, IrrigationDevice> devices = new ConcurrentHashMap<>();
    private final Map<String, IrrigationPlan> plans = new ConcurrentHashMap<>();
    private final Map<String, WeatherData> weatherDataMap = new ConcurrentHashMap<>();
    private final Map<String, WaterSource> waterSources = new ConcurrentHashMap<>();
    private final Map<String, DailyReport> dailyReports = new ConcurrentHashMap<>();
    private final Map<String, FaultEvent> faultEvents = new ConcurrentHashMap<>();

    private int maxConcurrentDevices = 3;

    public Map<String, Field> getFields() {
        return fields;
    }

    public Map<String, IrrigationDevice> getDevices() {
        return devices;
    }

    public Map<String, IrrigationPlan> getPlans() {
        return plans;
    }

    public Map<String, WeatherData> getWeatherDataMap() {
        return weatherDataMap;
    }

    public Map<String, WaterSource> getWaterSources() {
        return waterSources;
    }

    public Map<String, DailyReport> getDailyReports() {
        return dailyReports;
    }

    public int getMaxConcurrentDevices() {
        return maxConcurrentDevices;
    }

    public void setMaxConcurrentDevices(int maxConcurrentDevices) {
        this.maxConcurrentDevices = maxConcurrentDevices;
    }

    public List<Field> getFieldList() {
        return new ArrayList<>(fields.values());
    }

    public List<IrrigationDevice> getDeviceList() {
        return new ArrayList<>(devices.values());
    }

    public List<IrrigationPlan> getPlanList() {
        return new ArrayList<>(plans.values());
    }

    public Map<String, FaultEvent> getFaultEvents() {
        return faultEvents;
    }

    public List<FaultEvent> getFaultEventList() {
        List<FaultEvent> list = new ArrayList<>(faultEvents.values());
        list.sort((a, b) -> b.getFaultTime().compareTo(a.getFaultTime()));
        return list;
    }
}
