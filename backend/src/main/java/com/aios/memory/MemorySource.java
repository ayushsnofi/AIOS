package com.aios.memory;

public enum MemorySource {
    USER,
    ASSISTANT,
    EXTERNAL_SOURCE,
    SYSTEM;

    public String toMetadataValue() {
        return name();
    }

    public static MemorySource fromMetadataValue(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return MemorySource.valueOf(value.toUpperCase());
    }
}
