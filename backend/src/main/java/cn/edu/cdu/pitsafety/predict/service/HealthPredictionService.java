package cn.edu.cdu.pitsafety.predict.service;

import cn.edu.cdu.pitsafety.predict.dto.DeviceHealth;
import cn.edu.cdu.pitsafety.predict.dto.HealthTimelinePoint;
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
    private static final int TIMELINE_START_MONTH = -6;
    private static final int TIMELINE_END_MONTH = 6;

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
            h.setTimeline(buildTimeline(Collections.emptyMap(), 100));
            return h;
        }

        int pct = toInt(d.get("healthPercent"), 100);
        h.setHealthPercent(pct);
        h.setEstimatedRemainingHours(toInt(d.get("estimatedRemainingHours"), 8760));
        h.setConfidence(toDouble(d.get("confidence"), 0.5));
        h.setDirection(safeString(d.get("direction"), "stable"));
        h.setSlope(toDouble(d.get("slope"), 0));
        h.setLastValue(toDouble(d.get("lastValue"), 0));
        h.setStatus(statusFromPercent(pct));
        h.setTimeline(buildTimeline(d, pct));
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

    private List<HealthTimelinePoint> buildTimeline(Map<String, Object> d, int targetPct) {
        List<HealthTimelinePoint> points = new ArrayList<>();
        double confidence = toDouble(d.get("confidence"), 0.5);
        double anomalyScore = estimateAnomalyScore(d);

        for (int month = TIMELINE_START_MONTH; month <= TIMELINE_END_MONTH; month++) {
            int health = 100;
            if (month > 0) {
                double factor = month / (double) TIMELINE_END_MONTH;
                double curve = Math.max(0.65, 1.25 - confidence * 0.35 - anomalyScore * 0.25);
                double adjusted = Math.pow(factor, curve);
                health = (int) Math.round(100 - (100 - targetPct) * adjusted);
            }

            HealthTimelinePoint point = new HealthTimelinePoint();
            point.setMonthOffset((double) month);
            point.setHealthPercent(clampInt(health, 0, 100));
            point.setStatus(statusFromPercent(point.getHealthPercent()));
            point.setAnomalyScore(round2(anomalyScore));
            points.add(point);
        }

        return points;
    }

    private double estimateAnomalyScore(Map<String, Object> d) {
        double lastValue = Math.abs(toDouble(d.get("lastValue"), 0));
        double slope = Math.abs(toDouble(d.get("slope"), 0));
        double mae = Math.abs(toDouble(d.get("mae"), 0));
        double jumpCount = Math.abs(toDouble(d.get("anomalyJumpCount"), 0));

        double maeScore = clamp(mae / (lastValue + 1), 0, 1);
        double slopeScore = clamp(slope / (lastValue + 1) * 30, 0, 1);
        double jumpScore = clamp(jumpCount / 100, 0, 1);

        return clamp(maeScore * 0.35 + slopeScore * 0.35 + jumpScore * 0.30, 0, 1);
    }

    private String statusFromPercent(int pct) {
        return pct >= 80 ? "normal" : pct >= 50 ? "warning" : "danger";
    }

    private int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
