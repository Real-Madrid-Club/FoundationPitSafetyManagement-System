package cn.edu.cdu.pitsafety.predict.controller;

import cn.edu.cdu.pitsafety.common.Result;
import cn.edu.cdu.pitsafety.predict.dto.DeviceHealth;
import cn.edu.cdu.pitsafety.predict.service.HealthPredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthPredictionController {

    private final HealthPredictionService healthPredictionService;

    @GetMapping("/{sensorCode}")
    public Result<DeviceHealth> getHealth(@PathVariable String sensorCode) {
        return Result.success(healthPredictionService.getHealth(sensorCode));
    }

    @GetMapping("/all")
    public Result<List<DeviceHealth>> getAllHealth() {
        return Result.success(healthPredictionService.getAllHealth());
    }
}
