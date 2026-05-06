package cn.edu.cdu.pitsafety.ai.config;

import cn.edu.cdu.pitsafety.ai.config.DeepSeekProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DeepSeekConfig {

    @Bean
    public WebClient deepSeekWebClient(DeepSeekProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
