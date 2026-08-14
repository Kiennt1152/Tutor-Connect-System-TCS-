package com.tcs.module.ai.repository;

import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.enums.KnowledgeSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AiKnowledgeChunkRepository extends JpaRepository<AiKnowledgeChunk, Long> {
    Optional<AiKnowledgeChunk> findBySourceTypeAndSourceId(KnowledgeSourceType sourceType, String sourceId);
    List<AiKnowledgeChunk> findByActiveTrue();
}
