package com.aios.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aios.memory.vector-store-enabled", havingValue = "true", matchIfMissing = true)
public class VectorStoreConfig {

    private final AiosProperties aiosProperties;

    @Bean
    @Primary
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName("public")
                .vectorTableName("memories")
                .dimensions(aiosProperties.getMemory().getEmbeddingDimensions())
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(false)
                .maxDocumentBatchSize(1000)
                .build();
    }
}
