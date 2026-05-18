package com.irrigation.controller;

import com.irrigation.entity.IrrigationDevice;
import com.irrigation.enums.DeviceStatus;
import com.irrigation.service.IrrigationPlanService;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private IrrigationPlanService irrigationPlanService;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", new ArrayList<>(dataStorage.getDevices().values()));
        result.put("total", dataStorage.getDevices().size());
        result.put("maxConcurrent", dataStorage.getMaxConcurrentDevices());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        IrrigationDevice device = dataStorage.getDevices().get(id);
        if (device != null) {
            result.put("success", true);
            result.put("data", device);
        } else {
            result.put("success", false);
            result.put("message", "设备不存在");
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody IrrigationDevice device) {
        Map<String, Object> result = new HashMap<>();
        String id = "DEVICE_" + System.currentTimeMillis();
        device.setId(id);
        device.setStatus(DeviceStatus.IDLE);
        device.setCreateTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());
        dataStorage.getDevices().put(id, device);
        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", device);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody IrrigationDevice device) {
        Map<String, Object> result = new HashMap<>();
        if (!dataStorage.getDevices().containsKey(id)) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }
        IrrigationDevice existing = dataStorage.getDevices().get(id);
        device.setId(id);
        device.setCreateTime(existing.getCreateTime());
        device.setUpdateTime(LocalDateTime.now());
        dataStorage.getDevices().put(id, device);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", device);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        if (dataStorage.getDevices().remove(id) != null) {
            result.put("success", true);
            result.put("message", "删除成功");
        } else {
            result.put("success", false);
            result.put("message", "设备不存在");
        }
        return result;
    }

    @PostMapping("/{id}/fault")
    public Map<String, Object> reportFault(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        IrrigationDevice device = dataStorage.getDevices().get(id);
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }

        String faultMessage = body != null ? body.get("message") : "设备故障";

        Map<String, Object> reallocateResult = irrigationPlanService.reallocateResources(id, faultMessage);
        result.put("success", true);
        result.put("message", "故障已上报");
        result.put("reallocateResult", reallocateResult);
        result.put("device", dataStorage.getDevices().get(id));
        return result;
    }

    @GetMapping("/fault-events")
    public Map<String, Object> getFaultEvents() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", dataStorage.getFaultEventList());
        result.put("total", dataStorage.getFaultEvents().size());
        return result;
    }

    @PostMapping("/{id}/repair")
    public Map<String, Object> repair(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        IrrigationDevice device = dataStorage.getDevices().get(id);
        if (device == null) {
            result.put("success", false);
            result.put("message", "设备不存在");
            return result;
        }

        device.setStatus(DeviceStatus.IDLE);
        device.setFaultMessage(null);
        device.setFieldId(null);
        device.setLastMaintenanceTime(LocalDateTime.now());
        device.setUpdateTime(LocalDateTime.now());

        result.put("success", true);
        result.put("message", "设备已修复");
        result.put("device", device);
        return result;
    }

    @PutMapping("/max-concurrent/{num}")
    public Map<String, Object> setMaxConcurrent(@PathVariable int num) {
        Map<String, Object> result = new HashMap<>();
        dataStorage.setMaxConcurrentDevices(num);
        result.put("success", true);
        result.put("message", "最大并发设备数已设置为: " + num);
        return result;
    }
}
