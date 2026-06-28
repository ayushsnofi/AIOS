package com.aios.memory;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class MemoryIngestionResult {

    boolean allowedForVectorStore;
    boolean requiresHumanApproval;
    double trustScore;
    String sanitizedContent;
    @Builder.Default
    List<String> violations = new ArrayList<>();

    public static MemoryIngestionResult rejected(String reason) {
        return MemoryIngestionResult.builder()
                .allowedForVectorStore(false)
                .requiresHumanApproval(true)
                .trustScore(0.0)
                .sanitizedContent("")
                .violations(List.of(reason))
                .build();
    }
}
