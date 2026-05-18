package com.irrigation.controller;

import com.irrigation.entity.DailyReport;
import com.irrigation.entity.WaterSource;
import com.irrigation.entity.WeatherData;
import com.irrigation.service.ScheduledTaskService;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @GetMapping("/weather")
    public Map<String, Object> getWeather() {
        Map<String, Object> result = new HashMap<>();
        String today = LocalDate.now().toString();
        WeatherData weather = dataStorage.getWeatherDataMap().get(today);
        result.put("success", true);
        result.put("data", weather);
        return result;
    }

    @PostMapping("/weather/simulate")
    public Map<String, Object> simulateWeather() {
        return scheduledTaskService.simulateWeather();
    }

    @GetMapping("/water-sources")
    public Map<String, Object> getWaterSources() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", new ArrayList<>(dataStorage.getWaterSources().values()));
        return result;
    }

    @PutMapping("/water-sources/{id}")
    public Map<String, Object> updateWaterSource(@PathVariable String id, @RequestBody WaterSource ws) {
        Map<String, Object> result = new HashMap<>();
        WaterSource existing = dataStorage.getWaterSources().get(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "水源不存在");
            return result;
        }
        ws.setId(id);
        ws.setUpdateTime(LocalDateTime.now());
        ws.setCreateTime(existing.getCreateTime() != null ? existing.getCreateTime() : LocalDateTime.now());
        dataStorage.getWaterSources().put(id, ws);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", ws);
        return result;
    }

    @GetMapping("/daily-report")
    public Map<String, Object> getDailyReport() {
        Map<String, Object> result = new HashMap<>();
        String today = LocalDate.now().toString();
        DailyReport report = dataStorage.getDailyReports().get(today);
        if (report == null) {
            report = scheduledTaskService.generateDailyReport();
        }
        result.put("success", true);
        result.put("data", report);
        return result;
    }

    @PostMapping("/daily-report/generate")
    public Map<String, Object> generateReport() {
        Map<String, Object> result = new HashMap<>();
        DailyReport report = scheduledTaskService.generateDailyReport();
        result.put("success", true);
        result.put("message", "日报生成成功");
        result.put("data", report);
        return result;
    }

    @PostMapping("/update-status")
    public Map<String, Object> updateStatus() {
        Map<String, Object> result = new HashMap<>();
        scheduledTaskService.updateDeviceStatus();
        result.put("success", true);
        result.put("message", "状态已更新");
        return result;
    }

    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> overview = new HashMap<>();

        overview.put("totalFields", dataStorage.getFields().size());
        overview.put("totalDevices", dataStorage.getDevices().size());
        overview.put("runningDevices", dataStorage.getDevices().values().stream()
                .filter(d -> d.getStatus() == com.irrigation.enums.DeviceStatus.RUNNING).count());
        overview.put("faultDevices", dataStorage.getDevices().values().stream()
                .filter(d -> d.getStatus() == com.irrigation.enums.DeviceStatus.FAULT).count());
        overview.put("totalPlans", dataStorage.getPlans().size());
        overview.put("pendingPlans", dataStorage.getPlans().values().stream()
                .filter(p -> p.getStatus() == com.irrigation.enums.IrrigationPlanStatus.PENDING).count());
        overview.put("inProgressPlans", dataStorage.getPlans().values().stream()
                .filter(p -> p.getStatus() == com.irrigation.enums.IrrigationPlanStatus.IN_PROGRESS).count());

        double totalWater = dataStorage.getWaterSources().values().stream()
                .mapToDouble(WaterSource::getTotalCapacity).sum();
        double remainingWater = dataStorage.getWaterSources().values().stream()
                .mapToDouble(WaterSource::getRemainingCapacity).sum();
        overview.put("totalWater", totalWater);
        overview.put("remainingWater", remainingWater);
        overview.put("waterUsageRate", totalWater > 0 ? (1 - remainingWater / totalWater) : 0);

        String today = LocalDate.now().toString();
        WeatherData weather = dataStorage.getWeatherDataMap().get(today);
        overview.put("weather", weather);

        result.put("success", true);
        result.put("data", overview);
        return result;
    }
}
