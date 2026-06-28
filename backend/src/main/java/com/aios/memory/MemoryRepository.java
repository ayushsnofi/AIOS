package com.aios.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, UUID>, MemoryRepositoryCustom {
}
