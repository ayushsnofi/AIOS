package com.aios.memory;

import lombok.Value;

import java.util.UUID;

@Value
public class HybridMemoryMatch {

    UUID id;
    String content;
    MemoryMetadata metadata;
    double distance;
}
