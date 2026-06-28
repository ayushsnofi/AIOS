package com.aios.memory.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SemanticCacheEntry {

    private float[] embedding;
    private String combinedContext;
    private List<String> segmentIds;
    private long cachedAtEpochMs;
}
