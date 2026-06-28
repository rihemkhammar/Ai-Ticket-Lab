package com.genai.java.spring.rag.chunk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SemanticChunkRepository extends JpaRepository<SemanticChunk, Long> {

    @Modifying
    @Query("DELETE FROM SemanticChunk c WHERE c.articleId = :articleId")
    void deleteByArticleId(@Param("articleId") Long articleId);

    long countByArticleId(Long articleId);
}