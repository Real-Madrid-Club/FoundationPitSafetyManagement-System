package cn.edu.cdu.pitsafety.predict.service;

import cn.edu.cdu.pitsafety.predict.dto.DeviceHealth;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class HealthPredictionService {

    private Map<String, Map<String, Object>> modelData = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        try {
            InputStream is = new ClassPathResource("model_params.json").getInputStream();
            Map<String, Object> root = mapper.readValue(is, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> devices = (Map<String, Map<String, Object>>) root.get("devices");
            if (devices != null) modelData = devices;
            log.info("加载模型参数成功，共 {} 台设备", modelData.size());
        } catch (Exception e) {
            log.error("加载 model_params.json 失败: {}", e.getMessage());
        }
    }

    public DeviceHealth getHealth(String sensorCode) {
        DeviceHealth h = new DeviceHealth();
        h.setSensorCode(sensorCode);

        Map<String, Object> d = modelData.get(sensorCode);
        if (d == null) {
            h.setHealthPercent(100);
            h.setEstimatedRemainingHours(8760);
            h.setConfidence(0.5);
            h.setDirection("stable");
            h.setStatus("normal");
            return h;
        }

        int pct = toInt(d.get("healthPercent"), 100);
        h.setHealthPercent(pct);
        h.setEstimatedRemainingHours(toInt(d.get("estimatedRemainingHours"), 8760));
        h.setConfidence(toDouble(d.get("confidence"), 0.5));
        h.setDirection(safeString(d.get("direction"), "stable"));
        h.setSlope(toDouble(d.get("slope"), 0));
        h.setLastValue(toDouble(d.get("lastValue"), 0));
        h.setStatus(pct >= 80 ? "normal" : pct >= 50 ? "warning" : "danger");
        return h;
    }

    public List<DeviceHealth> getAllHealth() {
        List<DeviceHealth> list = new ArrayList<>();
        for (String code : modelData.keySet()) {
            list.add(getHealth(code));
        }
        list.sort((a, b) -> a.getHealthPercent().compareTo(b.getHealthPercent()));
        return list;
    }

    private int toInt(Object v, int def) {
        if (v instanceof Number) return ((Number) v).intValue();
        return def;
    }

    private double toDouble(Object v, double def) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }

    private String safeString(Object v, String def) {
        return v != null ? v.toString() : def;
    }
}
