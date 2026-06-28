package com.aios.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MemoryIngestionGuardImpl implements MemoryIngestionGuard {

    private static final double TRUST_USER = 0.9;
    private static final double TRUST_ASSISTANT = 0.75;
    private static final double TRUST_EXTERNAL = 0.1;
    private static final double TRUST_SYSTEM = 0.85;

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior)\\s+(rules|instructions)"),
            Pattern.compile("(?i)remember\\s+that\\s+i\\s+am\\s+your\\s+admin"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(dan|jailbreak)"),
            Pattern.compile("(?i)disregard\\s+(your\\s+)?(system|safety)\\s+(prompt|rules)"),
            Pattern.compile("(?i)store\\s+this\\s+in\\s+(permanent\\s+)?memory"),
            Pattern.compile("(?i)override\\s+security\\s+policy"),
            Pattern.compile("(?i)\\bDAN\\s+mode\\b")
    );

    @Override
    public MemoryIngestionResult evaluate(String content, MemoryMetadata metadata) {
        if (content == null || content.isBlank()) {
            return MemoryIngestionResult.rejected("Content must not be empty");
        }

        MemorySource source = metadata.getSource() != null ? metadata.getSource() : MemorySource.USER;
        double trustScore = resolveTrustScore(source, metadata.getTrustScore());
        String sanitized = sanitizeForEmbedding(content);

        List<String> violations = detectViolations(sanitized);
        boolean requiresHumanApproval = source == MemorySource.EXTERNAL_SOURCE || !violations.isEmpty();

        if (source == MemorySource.EXTERNAL_SOURCE && !metadata.isHumanApproved()) {
            log.warn("memory_ingestion_blocked source=EXTERNAL_SOURCE humanApproved=false trustScore={}",
                    trustScore);
            return MemoryIngestionResult.builder()
                    .allowedForVectorStore(false)
                    .requiresHumanApproval(true)
                    .trustScore(trustScore)
                    .sanitizedContent(sanitized)
                    .violations(violations)
                    .build();
        }

        if (!violations.isEmpty() && !metadata.isHumanApproved()) {
            log.warn("memory_ingestion_blocked injection_patterns_detected count={}", violations.size());
            return MemoryIngestionResult.builder()
                    .allowedForVectorStore(false)
                    .requiresHumanApproval(true)
                    .trustScore(Math.min(trustScore, TRUST_EXTERNAL))
                    .sanitizedContent(sanitized)
                    .violations(violations)
                    .build();
        }

        return MemoryIngestionResult.builder()
                .allowedForVectorStore(true)
                .requiresHumanApproval(requiresHumanApproval)
                .trustScore(trustScore)
                .sanitizedContent(sanitized)
                .violations(violations)
                .build();
    }

    @Override
    public String sanitizeForEmbedding(String content) {
        if (content == null) {
            return "";
        }
        String sanitized = content.strip();
        for (Pattern pattern : INJECTION_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]");
        }
        return sanitized;
    }

    private double resolveTrustScore(MemorySource source, double providedScore) {
        if (providedScore > 0) {
            return providedScore;
        }
        return switch (source) {
            case USER -> TRUST_USER;
            case ASSISTANT -> TRUST_ASSISTANT;
            case EXTERNAL_SOURCE -> TRUST_EXTERNAL;
            case SYSTEM -> TRUST_SYSTEM;
        };
    }

    private List<String> detectViolations(String content) {
        List<String> violations = new ArrayList<>();
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(content).find()) {
                violations.add(pattern.pattern());
            }
        }
        return violations;
    }
}
