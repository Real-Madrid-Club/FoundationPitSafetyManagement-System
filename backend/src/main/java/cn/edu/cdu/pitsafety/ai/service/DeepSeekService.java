package cn.edu.cdu.pitsafety.ai.service;

import cn.edu.cdu.pitsafety.ai.config.DeepSeekProperties;
import cn.edu.cdu.pitsafety.ai.dto.ChatRequest;
import cn.edu.cdu.pitsafety.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekService {

    private final DeepSeekProperties properties;
    private final WebClient webClient;

    private static final String SYSTEM_MAINTENANCE = """
            你是一个基坑工程设备维修专家助手。你精通以下设备的故障诊断与维修：
            - 测斜仪（倾角传感器）
            - 水位计（孔隙水压力计）
            - 土压力计
            - 钢筋应力计
            - 锚索测力计
            - 位移计
            请根据用户描述的故障现象，提供：
            1. 可能的故障原因分析
            2. 排查步骤建议
            3. 维修方案与注意事项
            如果问题超出你的知识范围，请诚实说明并建议联系设备厂商。
            """;

    private static final String SYSTEM_HEALTH = """
            你是一个基坑工程设备健康监测分析专家。你会收到设备的监测数据，
            请分析数据中的异常模式并给出评估：
            1. 数据是否存在异常跳变或趋势性偏移
            2. 判断设备当前健康状态（正常/关注/警告/危险）
            3. 可能的工程风险提示
            4. 建议的后续监测或处理措施
            你只基于提供的数据进行分析，不猜测未提供的信息。
            """;

    public Mono<String> chat(String userMessage, List<ChatRequest.Message> history) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(ChatRequest.Message.builder().role("system").content("你是一个专业的基坑安全管理系统AI助手，请用中文回答问题。").build());
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(ChatRequest.Message.builder().role("user").content(userMessage).build());
        return callDeepSeek(properties.getModel().getChat(), messages);
    }

    public Mono<String> maintenanceAssistant(String faultDescription, List<ChatRequest.Message> history) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(ChatRequest.Message.builder().role("system").content(SYSTEM_MAINTENANCE).build());
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(ChatRequest.Message.builder().role("user").content(faultDescription).build());
        return callDeepSeek(properties.getModel().getChat(), messages);
    }

    public Mono<String> healthMonitor(String deviceData, List<ChatRequest.Message> history) {
        List<ChatRequest.Message> messages = new ArrayList<>();
        messages.add(ChatRequest.Message.builder().role("system").content(SYSTEM_HEALTH).build());
        if (history != null && !history.isEmpty()) {
            messages.addAll(history);
        }
        messages.add(ChatRequest.Message.builder().role("user").content(deviceData).build());
        return callDeepSeek(properties.getModel().getChat(), messages);
    }

    private Mono<String> callDeepSeek(String model, List<ChatRequest.Message> messages) {
        ChatRequest request = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .temperature(0.7)
                .maxTokens(2048)
                .build();

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        ChatResponse.Choice.Message msg = response.getChoices().get(0).getMessage();
                        return msg != null ? msg.getContent() : "AI 未返回有效内容";
                    }
                    return "AI 未返回有效内容";
                })
                .doOnError(e -> log.error("DeepSeek API 调用失败: {}", e.getMessage()));
    }
}
