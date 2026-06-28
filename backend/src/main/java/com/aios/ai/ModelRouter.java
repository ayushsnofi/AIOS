package com.aios.ai;

import com.aios.config.AiosProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ModelRouter {

    private static final Map<String, String> MODEL_ALIASES = Map.of(
            "phi4", "phi4",
            "phi-4", "phi4",
            "qwen3", "qwen3",
            "qwen-3", "qwen3",
            "utility", "phi4",
            "main", "qwen3"
    );

    private static final Set<String> SUPPORTED_MODELS = Set.of("phi4", "qwen3");

    private final AiosProperties aiosProperties;

    public String resolve(String requestedModel) {
        if (!StringUtils.hasText(requestedModel)) {
            return aiosProperties.getAi().getDefaultModel();
        }
        String normalized = requestedModel.trim().toLowerCase(Locale.ROOT);
        String resolved = MODEL_ALIASES.getOrDefault(normalized, normalized);
        if (!SUPPORTED_MODELS.contains(resolved)) {
            return aiosProperties.getAi().getDefaultModel();
        }
        return resolved;
    }

    public String fallbackModel(String primaryModel) {
        String configuredFallback = aiosProperties.getAi().getFallbackModel();
        if (!primaryModel.equals(configuredFallback)) {
            return configuredFallback;
        }
        return aiosProperties.getAi().getDefaultModel();
    }
}
