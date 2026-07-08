package com.genai.java.spring.rag.indexing;

import com.genai.java.spring.knowledge.KnowledgeArticle;
import com.genai.java.spring.knowledge.KnowledgeArticleRepository;
import com.genai.java.spring.rag.chunk.ArticleChunkingService;
import com.genai.java.spring.rag.chunk.ArticleChunkingService.ArticleChunk;
import com.genai.java.spring.rag.chunk.SemanticChunk;
import com.genai.java.spring.rag.chunk.SemanticChunkRepository;
import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.embedding.EmbeddingService;

import com.genai.java.spring.rag.indexing.dto.ArticleIndexStatusResponse;

import com.genai.java.spring.rag.indexing.dto.IndexingSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ndexes every knowledge article into semantic_chunk:
 * chunk -> embed -> store, after wiping any previous chunks for that article.
 */
@Slf4j
@Service
public class ArticleIndexingService {

    private final KnowledgeArticleRepository articleRepository;
    private final ArticleChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final SemanticChunkRepository chunkRepository;
    private final VectorChunkDao vectorChunkDao;
    private final ObjectMapper objectMapper;

    public ArticleIndexingService(KnowledgeArticleRepository articleRepository,
                                  ArticleChunkingService chunkingService,
                                  EmbeddingService embeddingService,
                                  SemanticChunkRepository chunkRepository,
                                  VectorChunkDao vectorChunkDao,
                                  ObjectMapper objectMapper) {
        this.articleRepository = articleRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.chunkRepository = chunkRepository;
        this.vectorChunkDao = vectorChunkDao;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IndexingSummaryResponse indexAllArticles() {
        List<KnowledgeArticle> articles = articleRepository.findAll();

        int totalChunks = 0;
        for (KnowledgeArticle article : articles) {
            totalChunks += indexArticle(article);
        }

        log.info("Indexing complete: articlesIndexed={} chunksCreated={}", articles.size(), totalChunks);
        return new IndexingSummaryResponse(articles.size(), totalChunks);
    }


    /**
     * S4-BUG-01: real, DB-backed snapshot of how many articles/chunks
     * currently exist. Unlike the response of indexAllArticles(), this can
     * be called any time (page load, dashboard load, after navigating
     * back) and always reflects what is actually persisted.
     */
    public ArticleIndexStatusResponse getStatus() {
        long articlesTotal = articleRepository.count();
        long chunksTotal = chunkRepository.count();
        return new ArticleIndexStatusResponse(articlesTotal, chunksTotal);
    }

    private int indexArticle(KnowledgeArticle article) {
        // 1. Remove previous chunks for this article (reindex behavior).
        chunkRepository.deleteByArticleId(article.getId());

        // 2. Chunk the article content.
        List<ArticleChunk> chunks = chunkingService.chunk(article);

        // 3. Embed + persist each chunk.
        for (ArticleChunk chunk : chunks) {
            SemanticChunk entity = new SemanticChunk();
            entity.setArticleId(chunk.articleId());
            entity.setChunkIndex(chunk.chunkIndex());
            entity.setText(chunk.text());
            entity.setMetadataJson(toMetadataJson(chunk));
            entity.setCreatedAt(LocalDateTime.now());
            entity = chunkRepository.save(entity);

            float[] embedding = embeddingService.embed(chunk.text());
            vectorChunkDao.updateEmbedding(entity.getId(), embedding);
        }

        log.info("Indexed articleId={} title={} chunks={}", article.getId(), article.getTitle(), chunks.size());
        return chunks.size();
    }

    private String toMetadataJson(ArticleChunk chunk) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "articleId", chunk.articleId(),
                    "articleTitle", chunk.articleTitle(),
                    "category", chunk.category(),
                    "chunkIndex", chunk.chunkIndex()
            ));
        } catch (Exception e) {
            log.warn("Failed to serialize chunk metadata for articleId={}", chunk.articleId(), e);
            return null;
        }
    }
}