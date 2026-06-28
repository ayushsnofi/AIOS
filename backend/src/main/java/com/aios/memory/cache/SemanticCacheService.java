package com.aios.memory.cache;

import com.aios.config.AiosProperties;
import com.aios.memory.RetrievedMemoryContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final String INDEX_KEY = "aios:semantic:index";
    private static final String ENTRY_PREFIX = "aios:semantic:entry:";

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final AiosProperties aiosProperties;

    public Optional<RetrievedMemoryContext> lookup(String prompt) {
        float[] queryEmbedding = embeddingModel.embed(prompt);
        double maxDistance = aiosProperties.getMemory().getSemanticCacheMaxDistance();

        List<String> entryKeys = redisTemplate.opsForList().range(INDEX_KEY, 0, -1);
        if (entryKeys == null || entryKeys.isEmpty()) {
            return Optional.empty();
        }

        SemanticCacheEntry bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (String entryKey : entryKeys) {
            SemanticCacheEntry entry = readEntry(entryKey);
            if (entry == null || entry.getEmbedding() == null) {
                continue;
            }
            double distance = cosineDistance(queryEmbedding, entry.getEmbedding());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = entry;
            }
        }

        if (bestMatch != null && bestDistance < maxDistance) {
            log.debug("semantic_cache_hit distance={}", bestDistance);
            return Optional.of(RetrievedMemoryContext.builder()
                    .combinedContext(bestMatch.getCombinedContext())
                    .segmentIds(bestMatch.getSegmentIds())
                    .fromSemanticCache(true)
                    .build());
        }

        return Optional.empty();
    }

    public void store(String prompt, float[] embedding, RetrievedMemoryContext context) {
        if (context == null || !context.hasContext()) {
            return;
        }

        float[] vector = embedding != null ? embedding : embeddingModel.embed(prompt);
        SemanticCacheEntry entry = new SemanticCacheEntry(
                vector,
                context.getCombinedContext(),
                context.getSegmentIds(),
                System.currentTimeMillis()
        );

        String entryKey = ENTRY_PREFIX + System.nanoTime();
        try {
            redisTemplate.opsForValue().set(
                    entryKey,
                    objectMapper.writeValueAsString(entry),
                    Duration.ofSeconds(aiosProperties.getMemory().getSemanticCacheTtlSeconds())
            );
            redisTemplate.opsForList().leftPush(INDEX_KEY, entryKey);
            trimIndex();
        } catch (JsonProcessingException ex) {
            log.warn("semantic_cache_store_failed reason=serialization");
        }
    }

    private void trimIndex() {
        int maxEntries = aiosProperties.getMemory().getSemanticCacheMaxEntries();
        Long size = redisTemplate.opsForList().size(INDEX_KEY);
        if (size == null || size <= maxEntries) {
            return;
        }
        List<String> evicted = redisTemplate.opsForList().range(INDEX_KEY, maxEntries, -1);
        if (evicted != null) {
            for (String key : evicted) {
                redisTemplate.delete(key);
            }
        }
        redisTemplate.opsForList().trim(INDEX_KEY, 0, maxEntries - 1);
    }

    private SemanticCacheEntry readEntry(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SemanticCacheEntry.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    static double cosineDistance(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return 1.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
