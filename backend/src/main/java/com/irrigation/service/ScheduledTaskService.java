package com.irrigation.service;

import com.irrigation.entity.*;
import com.irrigation.enums.DeviceStatus;
import com.irrigation.enums.GrowthStage;
import com.irrigation.enums.IrrigationPlanStatus;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ScheduledTaskService {

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private IrrigationPlanService irrigationPlanService;

    public DailyReport generateDailyReport() {
        DailyReport report = new DailyReport();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        report.setDate(today);
        report.setGeneratedTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        List<Field> fields = dataStorage.getFieldList();
        report.setTotalFields(fields.size());

        int highRisk = 0, mediumRisk = 0, lowRisk = 0;
        for (Field field : fields) {
            int risk = calculateWaterRisk(field);
            if (risk >= 70) highRisk++;
            else if (risk >= 40) mediumRisk++;
            else lowRisk++;
        }
        report.setHighRiskFields(highRisk);
        report.setMediumRiskFields(mediumRisk);
        report.setLowRiskFields(lowRisk);

        List<IrrigationDevice> devices = dataStorage.getDeviceList();
        report.setTotalDevices(devices.size());
        long runningCount = devices.stream().filter(d -> d.getStatus() == DeviceStatus.RUNNING).count();
        report.setRunningDevices((int) runningCount);
        report.setDeviceLoadRate(devices.isEmpty() ? 0 : (double) runningCount / devices.size());

        double totalWater = dataStorage.getWaterSources().values().stream()
                .mapToDouble(WaterSource::getTotalCapacity)
                .sum();
        double remainingWater = dataStorage.getWaterSources().values().stream()
                .mapToDouble(WaterSource::getRemainingCapacity)
                .sum();
        report.setWaterRemaining(remainingWater);
        report.setWaterUsageToday(totalWater - remainingWater);

        dataStorage.getDailyReports().put(today, report);

        return report;
    }

    private int calculateWaterRisk(Field field) {
        GrowthStage stage = field.getGrowthStage();
        double threshold = stage.getWaterThreshold();
        double moisture = field.getSoilMoisture();
        double deficit = threshold - moisture;

        int risk = (int) (deficit * 100);

        if (stage == GrowthStage.FLOWERING || stage == GrowthStage.FRUITING) {
            risk += 20;
        }

        return Math.min(100, Math.max(0, risk));
    }

    public void updateDeviceStatus() {
        LocalDateTime now = LocalDateTime.now();
        for (IrrigationPlan plan : dataStorage.getPlans().values()) {
            if (plan.getStatus() == IrrigationPlanStatus.PENDING && plan.getStartTime().isBefore(now)) {
                plan.setStatus(IrrigationPlanStatus.IN_PROGRESS);
                plan.setUpdateTime(now);

                IrrigationDevice device = dataStorage.getDevices().get(plan.getDeviceId());
                if (device != null) {
                    device.setStatus(DeviceStatus.RUNNING);
                    device.setFieldId(plan.getFieldId());
                    device.setUpdateTime(now);
                }
            }

            if (plan.getStatus() == IrrigationPlanStatus.IN_PROGRESS && plan.getEndTime().isBefore(now)) {
                plan.setStatus(IrrigationPlanStatus.COMPLETED);
                plan.setUpdateTime(now);

                IrrigationDevice device = dataStorage.getDevices().get(plan.getDeviceId());
                if (device != null) {
                    device.setStatus(DeviceStatus.IDLE);
                    device.setFieldId(null);
                    device.setUpdateTime(now);
                }

                Field field = dataStorage.getFields().get(plan.getFieldId());
                if (field != null) {
                    double newMoisture = Math.min(1.0, field.getSoilMoisture() + plan.getWaterAmount() / 100);
                    field.setSoilMoisture(newMoisture);
                    field.setUpdateTime(now);
                }

                for (WaterSource ws : dataStorage.getWaterSources().values()) {
                    double newRemaining = Math.max(0, ws.getRemainingCapacity() - plan.getWaterAmount());
                    ws.setRemainingCapacity(newRemaining);
                    ws.setUpdateTime(now);
                }
            }
        }
    }

    public Map<String, Object> simulateWeather() {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();
        WeatherData weather = new WeatherData();
        weather.setDate(today);

        double temp = 25 + new Random().nextGaussian() * 8;
        weather.setTemperature(Math.round(temp * 10) / 10.0);
        weather.setHumidity(40 + new Random().nextInt(40));
        double rainfall = new Random().nextDouble() > 0.7 ? new Random().nextDouble() * 50 : 0;
        weather.setRainfall(Math.round(rainfall * 10) / 10.0);

        String[] conditions = {"晴天", "多云", "阴天", "小雨", "中雨"};
        if (rainfall > 20) weather.setWeatherCondition("中雨");
        else if (rainfall > 5) weather.setWeatherCondition("小雨");
        else if (temp > 35) weather.setWeatherCondition("高温");
        else weather.setWeatherCondition(conditions[new Random().nextInt(3)]);

        WeatherData yesterday = dataStorage.getWeatherDataMap().get(today.minusDays(1).toString());
        int consecutiveDays = 0;
        if (yesterday != null && yesterday.getTemperature() > 30 && temp > 30) {
            consecutiveDays = yesterday.getConsecutiveHighTempDays() + 1;
        } else if (temp > 30) {
            consecutiveDays = 1;
        }
        weather.setConsecutiveHighTempDays(consecutiveDays);

        dataStorage.getWeatherDataMap().put(today.toString(), weather);

        result.put("weather", weather);
        result.put("message", "天气数据已更新");

        for (Field field : dataStorage.getFieldList()) {
            double evaporation = temp * 0.002;
            double newMoisture = Math.max(0, field.getSoilMoisture() - evaporation + rainfall * 0.001);
            field.setSoilMoisture(Math.round(newMoisture * 1000) / 1000.0);
            field.setUpdateTime(LocalDateTime.now());
        }

        return result;
    }
}
