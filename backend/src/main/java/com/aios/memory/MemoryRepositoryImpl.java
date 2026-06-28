package com.aios.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MemoryRepositoryImpl implements MemoryRepositoryCustom {

    private static final String HYBRID_SEARCH_SQL = """
            SELECT id, content, metadata,
                   (embedding <=> ?::vector) AS distance
            FROM memories
            WHERE embedding IS NOT NULL
              AND COALESCE((metadata->>'trust_score')::float, 0) >= ?
              AND (
                    COALESCE(metadata->>'source', 'USER') != 'EXTERNAL_SOURCE'
                    OR COALESCE((metadata->>'human_approved')::boolean, false) = true
                  )
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<HybridMemoryMatch> findSimilarWithMetadataFilter(float[] queryEmbedding,
                                                                double minTrustScore,
                                                                int limit) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(
                HYBRID_SEARCH_SQL,
                (rs, rowNum) -> {
                    UUID id = UUID.fromString(rs.getString("id"));
                    String content = rs.getString("content");
                    Map<String, Object> metadataMap = parseMetadata(rs.getString("metadata"));
                    double distance = rs.getDouble("distance");
                    return new HybridMemoryMatch(id, content, MemoryMetadata.fromMap(metadataMap), distance);
                },
                vectorLiteral,
                minTrustScore,
                vectorLiteral,
                limit
        );
    }

    private Map<String, Object> parseMetadata(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}
