package com.genai.java.spring.rag.chunk;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Maps every column of semantic_chunk EXCEPT "embedding".
 *
 * Hibernate has no native mapping for the pgvector "vector" type without an
 * extra custom-type dependency, so — to keep this training implementation
 * simple — the embedding column is written/read separately through plain
 * "regular" columns and to delete old chunks before reindexing.
 */
@Entity
@Table(name = "semantic_chunk")
public class SemanticChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadataJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId()                            { return id; }

    public Long getArticleId()                     { return articleId; }
    public void setArticleId(Long v)               { this.articleId = v; }

    public Integer getChunkIndex()                  { return chunkIndex; }
    public void setChunkIndex(Integer v)            { this.chunkIndex = v; }

    public String getText()                         { return text; }
    public void setText(String v)                    { this.text = v; }

    public String getMetadataJson()                  { return metadataJson; }
    public void setMetadataJson(String v)            { this.metadataJson = v; }

    public LocalDateTime getCreatedAt()              { return createdAt; }
    public void setCreatedAt(LocalDateTime v)        { this.createdAt = v; }
}