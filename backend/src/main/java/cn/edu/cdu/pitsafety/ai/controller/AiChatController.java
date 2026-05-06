package cn.edu.cdu.pitsafety.ai.controller;

import cn.edu.cdu.pitsafety.ai.dto.AiChatRequest;
import cn.edu.cdu.pitsafety.ai.dto.ChatRequest;
import cn.edu.cdu.pitsafety.ai.service.DeepSeekService;
import cn.edu.cdu.pitsafety.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final DeepSeekService deepSeekService;

    @PostMapping("/chat")
    public Mono<Result<String>> chat(@RequestBody AiChatRequest request) {
        var history = request.getHistory() != null
                ? request.getHistory().stream()
                        .map(m -> ChatRequest.Message.builder().role(m.getRole()).content(m.getContent()).build())
                        .collect(Collectors.toList())
                : null;
        return deepSeekService.chat(request.getMessage(), history)
                .map(Result::success)
                .defaultIfEmpty(Result.error(500, "AI 服务响应异常"));
    }

    @PostMapping("/maintenance")
    public Mono<Result<String>> maintenanceAssistant(@RequestBody AiChatRequest request) {
        var history = request.getHistory() != null
                ? request.getHistory().stream()
                        .map(m -> ChatRequest.Message.builder().role(m.getRole()).content(m.getContent()).build())
                        .collect(Collectors.toList())
                : null;
        return deepSeekService.maintenanceAssistant(request.getMessage(), history)
                .map(Result::success)
                .defaultIfEmpty(Result.error(500, "AI 服务响应异常"));
    }

    @PostMapping("/health-monitor")
    public Mono<Result<String>> healthMonitor(@RequestBody AiChatRequest request) {
        var history = request.getHistory() != null
                ? request.getHistory().stream()
                        .map(m -> ChatRequest.Message.builder().role(m.getRole()).content(m.getContent()).build())
                        .collect(Collectors.toList())
                : null;
        return deepSeekService.healthMonitor(request.getMessage(), history)
                .map(Result::success)
                .defaultIfEmpty(Result.error(500, "AI 服务响应异常"));
    }
}
