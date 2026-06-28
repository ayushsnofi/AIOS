package com.aios.memory;

import java.util.List;

public interface MemoryRepositoryCustom {

    List<HybridMemoryMatch> findSimilarWithMetadataFilter(float[] queryEmbedding, double minTrustScore, int limit);
}
