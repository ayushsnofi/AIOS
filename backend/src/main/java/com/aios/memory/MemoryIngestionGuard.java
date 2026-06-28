package com.aios.memory;

public interface MemoryIngestionGuard {

    MemoryIngestionResult evaluate(String content, MemoryMetadata metadata);

    String sanitizeForEmbedding(String content);
}
