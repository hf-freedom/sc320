package com.irrigation.controller;

import com.irrigation.entity.IrrigationPlan;
import com.irrigation.service.IrrigationPlanService;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class IrrigationPlanController {

    @Autowired
    private DataStorage dataStorage;

    @Autowired
    private IrrigationPlanService irrigationPlanService;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", new ArrayList<>(dataStorage.getPlans().values()));
        result.put("total", dataStorage.getPlans().size());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        IrrigationPlan plan = dataStorage.getPlans().get(id);
        if (plan != null) {
            result.put("success", true);
            result.put("data", plan);
        } else {
            result.put("success", false);
            result.put("message", "计划不存在");
        }
        return result;
    }

    @PostMapping("/generate")
    public Map<String, Object> generate() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.List<IrrigationPlan> plans = irrigationPlanService.generateIrrigationPlans();
            result.put("success", true);
            result.put("message", "灌溉计划生成成功");
            result.put("data", plans);
            result.put("count", plans.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody IrrigationPlan plan) {
        plan.setId(id);
        return irrigationPlanService.validateAndUpdatePlan(plan);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        if (dataStorage.getPlans().remove(id) != null) {
            result.put("success", true);
            result.put("message", "删除成功");
        } else {
            result.put("success", false);
            result.put("message", "计划不存在");
        }
        return result;
    }

    @PostMapping("/{id}/execute")
    public Map<String, Object> execute(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        IrrigationPlan plan = dataStorage.getPlans().get(id);
        if (plan == null) {
            result.put("success", false);
            result.put("message", "计划不存在");
            return result;
        }
        plan.setStartTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        result.put("success", true);
        result.put("message", "计划已开始执行");
        result.put("data", plan);
        return result;
    }

    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        IrrigationPlan plan = dataStorage.getPlans().get(id);
        if (plan == null) {
            result.put("success", false);
            result.put("message", "计划不存在");
            return result;
        }
        plan.setStatus(com.irrigation.enums.IrrigationPlanStatus.CANCELLED);
        plan.setUpdateTime(LocalDateTime.now());
        result.put("success", true);
        result.put("message", "计划已取消");
        result.put("data", plan);
        return result;
    }
}
