package com.aios.memory;

import java.util.List;
import java.util.UUID;

public interface MemoryService {

    RetrievedMemoryContext retrieveRelevantContext(String prompt, int limit);

    Memory saveMemory(String content, MemoryMetadata metadata);

    List<Memory> findPendingApproval();
}
