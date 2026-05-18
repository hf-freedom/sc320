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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IrrigationPlanService {

    @Autowired
    private DataStorage dataStorage;

    public List<IrrigationPlan> generateIrrigationPlans() {
        List<Field> fields = dataStorage.getFieldList();
        Map<String, IrrigationDevice> devices = dataStorage.getDevices();
        Map<String, WaterSource> waterSources = dataStorage.getWaterSources();
        WeatherData weatherData = getTodayWeather();

        List<IrrigationDevice> availableDevices = devices.values().stream()
                .filter(d -> d.getStatus() == DeviceStatus.IDLE || d.getStatus() == DeviceStatus.RUNNING)
                .collect(Collectors.toList());

        double totalWaterAvailable = waterSources.values().stream()
                .mapToDouble(WaterSource::getRemainingCapacity)
                .sum();

        List<FieldWithPriority> prioritizedFields = new ArrayList<>();
        for (Field field : fields) {
            int priority = calculatePriority(field, weatherData);
            prioritizedFields.add(new FieldWithPriority(field, priority));
        }

        prioritizedFields.sort((a, b) -> b.priority - a.priority);

        List<IrrigationPlan> plans = new ArrayList<>();
        double remainingWater = totalWaterAvailable;
        int usedDevices = 0;
        int maxConcurrent = dataStorage.getMaxConcurrentDevices();

        for (FieldWithPriority fp : prioritizedFields) {
            Field field = fp.field;
            if (usedDevices >= maxConcurrent) {
                break;
            }

            double requiredWater = calculateRequiredWater(field, weatherData);
            if (requiredWater <= 0) {
                continue;
            }

            if (remainingWater < requiredWater) {
                continue;
            }

            IrrigationDevice availableDevice = availableDevices.stream()
                    .filter(d -> d.getStatus() == DeviceStatus.IDLE)
                    .findFirst()
                    .orElse(null);

            if (availableDevice == null) {
                continue;
            }

            IrrigationPlan plan = createPlan(field, availableDevice, requiredWater, fp.priority, weatherData);
            plans.add(plan);

            dataStorage.getPlans().put(plan.getId(), plan);
            remainingWater -= requiredWater;
            usedDevices++;

            availableDevice.setStatus(DeviceStatus.RUNNING);
            availableDevice.setFieldId(field.getId());
            availableDevice.setUpdateTime(LocalDateTime.now());
        }

        return plans;
    }

    private int calculatePriority(Field field, WeatherData weather) {
        int priority = 0;

        GrowthStage stage = field.getGrowthStage();
        double threshold = stage.getWaterThreshold();
        double moisture = field.getSoilMoisture();
        double moistureDeficit = threshold - moisture;

        if (moistureDeficit > 0.3) {
            priority += 40;
        } else if (moistureDeficit > 0.2) {
            priority += 30;
        } else if (moistureDeficit > 0.1) {
            priority += 20;
        } else if (moistureDeficit > 0) {
            priority += 10;
        }

        if (stage == GrowthStage.FLOWERING || stage == GrowthStage.FRUITING) {
            priority += 25;
        } else if (stage == GrowthStage.VEGETATIVE) {
            priority += 15;
        }

        priority += field.getLevel() * 10;

        priority += (int) (field.getCropValue() / 100);

        if (weather != null) {
            if (weather.getConsecutiveHighTempDays() >= 3) {
                priority += 20;
            } else if (weather.getConsecutiveHighTempDays() >= 1) {
                priority += 10;
            }

            if (weather.getTemperature() > 35) {
                priority += 15;
            } else if (weather.getTemperature() > 30) {
                priority += 10;
            }

            if (weather.getRainfall() > 20) {
                priority = Math.max(0, priority - 30);
            }
        }

        return Math.min(100, priority);
    }

    private double calculateRequiredWater(Field field, WeatherData weather) {
        GrowthStage stage = field.getGrowthStage();
        double threshold = stage.getWaterThreshold();
        double currentMoisture = field.getSoilMoisture();
        double area = field.getArea();
        double cropCoefficient = field.getCropType().getWaterCoefficient();

        double moistureDeficit = threshold - currentMoisture;
        if (moistureDeficit <= 0) {
            return 0;
        }

        double baseWater = moistureDeficit * area * 10;

        baseWater *= cropCoefficient;

        if (weather != null) {
            double tempFactor = 1 + (weather.getTemperature() - 25) * 0.02;
            baseWater *= Math.max(0.8, tempFactor);

            baseWater = Math.max(0, baseWater - weather.getRainfall() * 0.5);
        }

        return Math.round(baseWater * 100) / 100.0;
    }

    private IrrigationPlan createPlan(Field field, IrrigationDevice device, double waterAmount, int priority, WeatherData weather) {
        IrrigationPlan plan = new IrrigationPlan();
        plan.setId("PLAN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 4));
        plan.setFieldId(field.getId());
        plan.setDeviceId(device.getId());
        plan.setStartTime(LocalDateTime.now());
        plan.setEndTime(LocalDateTime.now().plusMinutes((long) (waterAmount * 10)));
        plan.setWaterAmount(waterAmount);
        plan.setPriority(priority);
        plan.setStatus(IrrigationPlanStatus.PENDING);
        plan.setReason(generateReason(field, weather, priority));
        plan.setManualModified(false);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        return plan;
    }

    private String generateReason(Field field, WeatherData weather, int priority) {
        StringBuilder sb = new StringBuilder();
        sb.append("作物类型:").append(field.getCropType().getName());
        sb.append(",生长期:").append(field.getGrowthStage().getName());
        sb.append(",土壤湿度:").append(String.format("%.1f%%", field.getSoilMoisture() * 100));
        if (weather != null) {
            sb.append(",气温:").append(weather.getTemperature()).append("℃");
            if (weather.getConsecutiveHighTempDays() > 0) {
                sb.append(",连续高温").append(weather.getConsecutiveHighTempDays()).append("天");
            }
        }
        sb.append(",优先级:").append(priority);
        return sb.toString();
    }

    private WeatherData getTodayWeather() {
        String today = LocalDate.now().toString();
        return dataStorage.getWeatherDataMap().get(today);
    }

    public Map<String, Object> reallocateResources(String failedDeviceId, String faultMessage) {
        IrrigationDevice failedDevice = dataStorage.getDevices().get(failedDeviceId);
        Map<String, Object> result = new HashMap<>();

        if (failedDevice == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }

        FaultEvent event = new FaultEvent();
        String eventId = "FAULT_" + System.currentTimeMillis();
        event.setId(eventId);
        event.setDeviceId(failedDeviceId);
        event.setDeviceName(failedDevice.getName());
        event.setFaultMessage(faultMessage != null ? faultMessage : "设备故障");
        event.setFaultTime(LocalDateTime.now());
        event.setResolved(false);

        failedDevice.setStatus(DeviceStatus.FAULT);
        failedDevice.setFaultMessage(faultMessage);
        failedDevice.setUpdateTime(LocalDateTime.now());

        String affectedFieldId = failedDevice.getFieldId();
        event.setAffectedFieldId(affectedFieldId);

        List<IrrigationPlan> affectedPlans = dataStorage.getPlans().values().stream()
                .filter(p -> p.getDeviceId().equals(failedDeviceId)
                        && (p.getStatus() == IrrigationPlanStatus.PENDING || p.getStatus() == IrrigationPlanStatus.IN_PROGRESS))
                .collect(Collectors.toList());

        for (IrrigationPlan plan : affectedPlans) {
            plan.setStatus(IrrigationPlanStatus.PAUSED);
            plan.setUpdateTime(LocalDateTime.now());
            plan.setReason(plan.getReason() + " [设备" + failedDevice.getName() + "故障，已暂停]");
        }

        event.setPausedPlansCount(affectedPlans.size());

        List<IrrigationDevice> availableDevices = dataStorage.getDevices().values().stream()
                .filter(d -> d.getStatus() == DeviceStatus.IDLE)
                .collect(Collectors.toList());

        List<IrrigationPlan> reallocatedPlans = new ArrayList<>();
        List<String> reallocatedPlanIds = new ArrayList<>();
        IrrigationDevice firstNewDevice = null;

        for (IrrigationPlan pausedPlan : affectedPlans) {
            if (availableDevices.isEmpty()) {
                break;
            }

            IrrigationDevice newDevice = availableDevices.remove(0);
            if (firstNewDevice == null) {
                firstNewDevice = newDevice;
            }

            pausedPlan.setDeviceId(newDevice.getId());
            pausedPlan.setStatus(IrrigationPlanStatus.PENDING);
            pausedPlan.setUpdateTime(LocalDateTime.now());
            pausedPlan.setReason(pausedPlan.getReason().replace("[设备" + failedDevice.getName() + "故障，已暂停]",
                    "[设备故障重新分配: " + failedDevice.getName() + " → " + newDevice.getName() + "]"));

            newDevice.setStatus(DeviceStatus.RUNNING);
            newDevice.setFieldId(pausedPlan.getFieldId());
            newDevice.setUpdateTime(LocalDateTime.now());

            reallocatedPlans.add(pausedPlan);
            reallocatedPlanIds.add(pausedPlan.getId());
        }

        event.setReallocatedPlansCount(reallocatedPlans.size());
        event.setReallocatedPlanIds(reallocatedPlanIds);
        if (firstNewDevice != null) {
            event.setNewDeviceId(firstNewDevice.getId());
            event.setNewDeviceName(firstNewDevice.getName());
        }

        if (reallocatedPlans.size() == affectedPlans.size() && !affectedPlans.isEmpty()) {
            event.setResolved(true);
            event.setResolvedTime(LocalDateTime.now());
            event.setRemarks("所有受影响计划已成功重新分配");
        } else if (affectedPlans.isEmpty()) {
            event.setResolved(true);
            event.setResolvedTime(LocalDateTime.now());
            event.setRemarks("设备故障时无正在执行的灌溉计划");
        } else {
            event.setRemarks("部分计划未能重新分配，可用设备不足");
        }

        failedDevice.setFieldId(null);

        dataStorage.getFaultEvents().put(eventId, event);

        result.put("success", true);
        result.put("message", "资源重新分配完成");
        result.put("event", event);
        result.put("affectedFieldId", affectedFieldId);
        result.put("pausedPlans", affectedPlans.size());
        result.put("reallocatedPlans", reallocatedPlans.size());
        result.put("plans", reallocatedPlans);
        result.put("unallocatedPlans", affectedPlans.size() - reallocatedPlans.size());

        return result;
    }

    public Map<String, Object> validateAndUpdatePlan(IrrigationPlan updatedPlan) {
        Map<String, Object> result = new HashMap<>();
        List<String> conflicts = new ArrayList<>();

        IrrigationPlan existingPlan = dataStorage.getPlans().get(updatedPlan.getId());
        if (existingPlan == null) {
            result.put("success", false);
            result.put("message", "计划不存在");
            return result;
        }

        long overlappingRunning = dataStorage.getPlans().values().stream()
                .filter(p -> !p.getId().equals(updatedPlan.getId())
                        && p.getStatus() == IrrigationPlanStatus.IN_PROGRESS
                        && p.getDeviceId().equals(updatedPlan.getDeviceId()))
                .count();

        long overlappingPending = dataStorage.getPlans().values().stream()
                .filter(p -> !p.getId().equals(updatedPlan.getId())
                        && p.getStatus() == IrrigationPlanStatus.PENDING
                        && p.getDeviceId().equals(updatedPlan.getDeviceId())
                        && p.getStartTime().isBefore(updatedPlan.getEndTime())
                        && p.getEndTime().isAfter(updatedPlan.getStartTime()))
                .count();

        if (overlappingRunning > 0) {
            conflicts.add("设备正在执行其他任务");
        }
        if (overlappingPending > 0) {
            conflicts.add("设备在该时段已有待执行计划");
        }

        double totalWaterNeeded = dataStorage.getPlans().values().stream()
                .filter(p -> p.getStatus() == IrrigationPlanStatus.PENDING || p.getStatus() == IrrigationPlanStatus.IN_PROGRESS)
                .mapToDouble(IrrigationPlan::getWaterAmount)
                .sum();

        double totalWaterAvailable = dataStorage.getWaterSources().values().stream()
                .mapToDouble(WaterSource::getRemainingCapacity)
                .sum();

        double newWaterNeeded = totalWaterNeeded - existingPlan.getWaterAmount() + updatedPlan.getWaterAmount();
        if (newWaterNeeded > totalWaterAvailable) {
            conflicts.add("水源不足，需要水量: " + String.format("%.2f", newWaterNeeded) + "m³，可用: " + String.format("%.2f", totalWaterAvailable) + "m³");
        }

        if (!conflicts.isEmpty()) {
            result.put("success", false);
            result.put("message", "校验失败");
            result.put("conflicts", conflicts);
            return result;
        }

        existingPlan.setStartTime(updatedPlan.getStartTime());
        existingPlan.setEndTime(updatedPlan.getEndTime());
        existingPlan.setWaterAmount(updatedPlan.getWaterAmount());
        existingPlan.setDeviceId(updatedPlan.getDeviceId());
        existingPlan.setPriority(updatedPlan.getPriority());
        existingPlan.setManualModified(true);
        existingPlan.setUpdateTime(LocalDateTime.now());

        result.put("success", true);
        result.put("message", "计划更新成功");
        result.put("plan", existingPlan);

        return result;
    }

    private static class FieldWithPriority {
        Field field;
        int priority;

        FieldWithPriority(Field field, int priority) {
            this.field = field;
            this.priority = priority;
        }
    }
}
