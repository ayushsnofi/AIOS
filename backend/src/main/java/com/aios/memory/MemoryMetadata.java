package com.aios.memory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMetadata {

    private MemorySource source;
    private double trustScore;
    private boolean humanApproved;
    private Instant timestamp;
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("source", source != null ? source.toMetadataValue() : MemorySource.USER.toMetadataValue());
        map.put("trust_score", trustScore);
        map.put("human_approved", humanApproved);
        map.put("timestamp", timestamp != null ? timestamp.toString() : Instant.now().toString());
        if (tags != null && !tags.isEmpty()) {
            map.put("tags", tags);
        }
        return map;
    }

    public static MemoryMetadata fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return MemoryMetadata.builder().source(MemorySource.USER).trustScore(0.5).build();
        }
        MemorySource source = MemorySource.fromMetadataValue(String.valueOf(map.getOrDefault("source", "USER")));
        double trustScore = parseDouble(map.get("trust_score"), 0.5);
        boolean humanApproved = Boolean.parseBoolean(String.valueOf(map.getOrDefault("human_approved", false)));
        Instant timestamp = map.containsKey("timestamp")
                ? Instant.parse(String.valueOf(map.get("timestamp")))
                : Instant.now();
        @SuppressWarnings("unchecked")
        List<String> tags = map.containsKey("tags") ? (List<String>) map.get("tags") : List.of();
        return MemoryMetadata.builder()
                .source(source)
                .trustScore(trustScore)
                .humanApproved(humanApproved)
                .timestamp(timestamp)
                .tags(tags)
                .build();
    }

    private static double parseDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
