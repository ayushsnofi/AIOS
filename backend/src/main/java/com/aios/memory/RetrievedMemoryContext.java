package com.aios.memory;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class RetrievedMemoryContext {

    String combinedContext;
    @Builder.Default
    List<String> segmentIds = List.of();
    boolean fromSemanticCache;

    public static RetrievedMemoryContext empty() {
        return RetrievedMemoryContext.builder()
                .combinedContext("")
                .segmentIds(Collections.emptyList())
                .fromSemanticCache(false)
                .build();
    }

    public boolean hasContext() {
        return combinedContext != null && !combinedContext.isBlank();
    }
}
