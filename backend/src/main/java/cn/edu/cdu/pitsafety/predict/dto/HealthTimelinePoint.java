package cn.edu.cdu.pitsafety.predict.dto;

import lombok.Data;

@Data
public class HealthTimelinePoint {
    private Double monthOffset;
    private Integer healthPercent;
    private String status;
    private Double anomalyScore;
}
