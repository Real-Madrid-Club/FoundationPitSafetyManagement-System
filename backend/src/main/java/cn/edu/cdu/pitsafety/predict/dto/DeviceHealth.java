package cn.edu.cdu.pitsafety.predict.dto;

import lombok.Data;

@Data
public class DeviceHealth {
    private String sensorCode;
    private Integer healthPercent;
    private Integer estimatedRemainingHours;
    private Double confidence;
    private String direction;
    private String status;      // normal / warning / danger
    private Double slope;
    private Double lastValue;
}
