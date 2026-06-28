package com.aios.memory;

import com.aios.config.AiosProperties;
import com.aios.memory.cache.SemanticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private final MemoryRepository memoryRepository;
    private final MemoryIngestionGuard memoryIngestionGuard;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final SemanticCacheService semanticCacheService;
    private final AiosProperties aiosProperties;

    @Override
    public RetrievedMemoryContext retrieveRelevantContext(String prompt, int limit) {
        int effectiveLimit = limit > 0 ? limit : aiosProperties.getMemory().getRetrievalLimit();

        Optional<RetrievedMemoryContext> cached = semanticCacheService.lookup(prompt);
        if (cached.isPresent()) {
            log.debug("memory_retrieval semantic_cache_hit");
            return cached.get();
        }

        float[] queryEmbedding = embeddingModel.embed(prompt);
        double minTrust = aiosProperties.getMemory().getMinTrustScore();

        List<HybridMemoryMatch> matches = memoryRepository.findSimilarWithMetadataFilter(
                queryEmbedding,
                minTrust,
                effectiveLimit
        );

        if (matches.isEmpty()) {
            return RetrievedMemoryContext.empty();
        }

        List<String> segmentIds = matches.stream()
                .map(match -> match.getId().toString())
                .toList();

        String combined = matches.stream()
                .map(match -> "- " + match.getContent())
                .collect(Collectors.joining("\n"));

        RetrievedMemoryContext context = RetrievedMemoryContext.builder()
                .combinedContext(combined)
                .segmentIds(segmentIds)
                .fromSemanticCache(false)
                .build();

        semanticCacheService.store(prompt, queryEmbedding, context);
        log.debug("memory_retrieval segments={} from_cache=false", segmentIds.size());
        return context;
    }

    @Override
    @Transactional
    public Memory saveMemory(String content, MemoryMetadata metadata) {
        MemoryMetadata enriched = enrichMetadata(metadata);
        MemoryIngestionResult ingestion = memoryIngestionGuard.evaluate(content, enriched);
        enriched.setTrustScore(ingestion.getTrustScore());

        if (ingestion.isAllowedForVectorStore()) {
            UUID id = UUID.randomUUID();
            Document document = new Document(id.toString(), ingestion.getSanitizedContent(), enriched.toMap());
            vectorStore.add(List.of(document));
            log.info("memory_saved id={} vector_store=true trust_score={}", id, ingestion.getTrustScore());

            Memory memory = new Memory();
            memory.setId(id);
            memory.setContent(ingestion.getSanitizedContent());
            memory.setMetadata(enriched.toMap());
            memory.setCreatedAt(Instant.now());
            return memory;
        }

        Memory pending = new Memory();
        pending.setContent(ingestion.getSanitizedContent());
        pending.setMetadata(enriched.toMap());
        Memory saved = memoryRepository.save(pending);
        log.warn("memory_saved id={} vector_store=false requires_approval={}",
                saved.getId(), ingestion.isRequiresHumanApproval());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Memory> findPendingApproval() {
        return memoryRepository.findAll().stream()
                .filter(memory -> {
                    MemoryMetadata meta = memory.parsedMetadata();
                    return meta.getSource() == MemorySource.EXTERNAL_SOURCE && !meta.isHumanApproved();
                })
                .toList();
    }

    private MemoryMetadata enrichMetadata(MemoryMetadata metadata) {
        MemoryMetadata base = metadata != null ? metadata : MemoryMetadata.builder().build();
        if (base.getTimestamp() == null) {
            base.setTimestamp(Instant.now());
        }
        if (base.getSource() == null) {
            base.setSource(MemorySource.USER);
        }
        return base;
    }
}
