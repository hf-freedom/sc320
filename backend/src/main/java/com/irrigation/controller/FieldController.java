package com.irrigation.controller;

import com.irrigation.entity.Field;
import com.irrigation.storage.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/fields")
public class FieldController {

    @Autowired
    private DataStorage dataStorage;

    @GetMapping
    public Map<String, Object> list() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", new ArrayList<>(dataStorage.getFields().values()));
        result.put("total", dataStorage.getFields().size());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        Field field = dataStorage.getFields().get(id);
        if (field != null) {
            result.put("success", true);
            result.put("data", field);
        } else {
            result.put("success", false);
            result.put("message", "地块不存在");
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Field field) {
        Map<String, Object> result = new HashMap<>();
        String id = "FIELD_" + System.currentTimeMillis();
        field.setId(id);
        field.setCreateTime(LocalDateTime.now());
        field.setUpdateTime(LocalDateTime.now());
        dataStorage.getFields().put(id, field);
        result.put("success", true);
        result.put("message", "添加成功");
        result.put("data", field);
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable String id, @RequestBody Field field) {
        Map<String, Object> result = new HashMap<>();
        if (!dataStorage.getFields().containsKey(id)) {
            result.put("success", false);
            result.put("message", "地块不存在");
            return result;
        }
        field.setId(id);
        field.setUpdateTime(LocalDateTime.now());
        Field existing = dataStorage.getFields().get(id);
        field.setCreateTime(existing.getCreateTime());
        dataStorage.getFields().put(id, field);
        result.put("success", true);
        result.put("message", "更新成功");
        result.put("data", field);
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        if (dataStorage.getFields().remove(id) != null) {
            result.put("success", true);
            result.put("message", "删除成功");
        } else {
            result.put("success", false);
            result.put("message", "地块不存在");
        }
        return result;
    }
}
