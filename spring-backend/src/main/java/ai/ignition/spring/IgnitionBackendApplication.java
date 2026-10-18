package ai.ignition.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * IgnitionAI Spring Boot Gateway Service
 * Bridges Rahul's Android App to the Python FastAPI ML Diagnostics Service
 * (serving the Real Hybrid XGBoost + Physics Invariants model).
 */
@SpringBootApplication
public class IgnitionBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(IgnitionBackendApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
