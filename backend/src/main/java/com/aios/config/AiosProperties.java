package com.aios.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "aios")
public class AiosProperties {

    private Security security = new Security();
    private Ai ai = new Ai();
    private Cors cors = new Cors();
    private Memory memory = new Memory();

    @Data
    public static class Memory {
        private String embeddingModel = "nomic-embed-text";
        private int embeddingDimensions = 1536;
        private int retrievalLimit = 5;
        private double minTrustScore = 0.5;
        private double semanticCacheMaxDistance = 0.05;
        private int semanticCacheMaxEntries = 100;
        private long semanticCacheTtlSeconds = 3600;
    }

    @Data
    public static class Security {
        private String apiKeyHeader = "X-API-Key";
        private String placeholderApiKey = "dev-api-key-change-me";
    }

    @Data
    public static class Ai {
        private String defaultModel = "phi4";
        private String fallbackModel = "qwen3";
        private int maxRetries = 2;
        private long retryDelayMs = 500;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}
