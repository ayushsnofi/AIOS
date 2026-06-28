package com.aios.config;

import com.aios.memory.MemoryService;
import com.aios.memory.RetrievedMemoryContext;
import com.aios.memory.cache.SemanticCacheService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
@Profile("test")
public class TestMemoryConfig {

    @Bean
    @Primary
    MemoryService memoryService() {
        MemoryService service = mock(MemoryService.class);
        when(service.retrieveRelevantContext(anyString(), anyInt()))
                .thenReturn(RetrievedMemoryContext.empty());
        return service;
    }

    @Bean
    @Primary
    SemanticCacheService semanticCacheService() {
        return mock(SemanticCacheService.class);
    }

    @Bean
    @Primary
    EmbeddingModel embeddingModel() {
        return mock(EmbeddingModel.class);
    }

    @Bean
    @Primary
    VectorStore vectorStore() {
        return mock(VectorStore.class);
    }
}
