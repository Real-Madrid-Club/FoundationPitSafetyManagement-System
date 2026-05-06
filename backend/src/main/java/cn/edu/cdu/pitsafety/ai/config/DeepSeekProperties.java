package cn.edu.cdu.pitsafety.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private String apiKey;
    private String baseUrl = "https://api.deepseek.com";

    private ModelConfig model = new ModelConfig();

    @Data
    public static class ModelConfig {
        private String chat = "deepseek-chat";
        private String reasoner = "deepseek-reasoner";
    }
}
