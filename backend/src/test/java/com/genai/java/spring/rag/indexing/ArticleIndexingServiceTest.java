package com.genai.java.spring.rag.indexing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.java.spring.knowledge.KnowledgeArticle;
import com.genai.java.spring.knowledge.KnowledgeArticleRepository;
import com.genai.java.spring.rag.chunk.ArticleChunkingService;
import com.genai.java.spring.rag.chunk.ArticleChunkingService.ArticleChunk;
import com.genai.java.spring.rag.chunk.SemanticChunk;
import com.genai.java.spring.rag.chunk.SemanticChunkRepository;
import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.embedding.EmbeddingService;
import com.genai.java.spring.rag.indexing.dto.IndexingSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ArticleIndexingService.

 * Covers:
 * - indexing deletes old chunks before re-indexing (reindex behavior)
 * - indexing creates chunks for seeded articles
 * - summary counts are correct
 * - embedding is called once per chunk
 * - embedding is stored via vectorChunkDao
 */
class ArticleIndexingServiceTest {

    private KnowledgeArticleRepository articleRepository;
    private ArticleChunkingService chunkingService;
    private EmbeddingService embeddingService;
    private SemanticChunkRepository chunkRepository;
    private VectorChunkDao vectorChunkDao;

    private ArticleIndexingService service;

    @BeforeEach
    void setUp() {
        articleRepository  = mock(KnowledgeArticleRepository.class);
        chunkingService    = mock(ArticleChunkingService.class);
        embeddingService   = mock(EmbeddingService.class);
        chunkRepository    = mock(SemanticChunkRepository.class);
        vectorChunkDao     = mock(VectorChunkDao.class);

        service = new ArticleIndexingService(
                articleRepository, chunkingService, embeddingService,
                chunkRepository, vectorChunkDao, new ObjectMapper());

        // Default: embedding returns a dummy vector
        when(embeddingService.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        // Default: save returns the entity with a non-null id
        when(chunkRepository.save(any(SemanticChunk.class))).thenAnswer(inv -> {
            SemanticChunk sc = inv.getArgument(0);
            setId(sc, 99L);
            return sc;
        });
    }

    // -----------------------------------------------------------------------
    // 1. indexing deletes old chunks before re-indexing
    // -----------------------------------------------------------------------

    @Test
    void indexAllArticles_deletesOldChunks_beforeReindexing() {
        KnowledgeArticle article = article(1L, "Motor", "MOTOR", "Some content");
        when(articleRepository.findAll()).thenReturn(List.of(article));
        when(chunkingService.chunk(article)).thenReturn(List.of(
                new ArticleChunk(1L, 0, "Some content", "Motor", "MOTOR")));

        service.indexAllArticles();

        // delete must be called BEFORE save
        var inOrder = inOrder(chunkRepository);
        inOrder.verify(chunkRepository).deleteByArticleId(1L);
        inOrder.verify(chunkRepository).save(any());
    }

    @Test
    void indexAllArticles_deletesOldChunks_forEachArticle() {
        KnowledgeArticle a1 = article(1L, "Motor", "MOTOR", "Content 1");
        KnowledgeArticle a2 = article(2L, "Pump", "PUMP", "Content 2");
        when(articleRepository.findAll()).thenReturn(List.of(a1, a2));
        when(chunkingService.chunk(a1)).thenReturn(List.of(chunk(1L, 0)));
        when(chunkingService.chunk(a2)).thenReturn(List.of(chunk(2L, 0)));

        service.indexAllArticles();

        verify(chunkRepository).deleteByArticleId(1L);
        verify(chunkRepository).deleteByArticleId(2L);
    }

    // -----------------------------------------------------------------------
    // 2. indexing creates chunks for seeded articles
    // -----------------------------------------------------------------------

    @Test
    void indexAllArticles_createsChunks_forAllArticles() {
        KnowledgeArticle a1 = article(1L, "Motor", "MOTOR", "Content 1");
        KnowledgeArticle a2 = article(2L, "Pump", "PUMP", "Content 2");
        when(articleRepository.findAll()).thenReturn(List.of(a1, a2));
        when(chunkingService.chunk(a1)).thenReturn(List.of(chunk(1L, 0), chunk(1L, 1)));
        when(chunkingService.chunk(a2)).thenReturn(List.of(chunk(2L, 0)));

        service.indexAllArticles();

        // 3 chunks saved total
        verify(chunkRepository, times(3)).save(any(SemanticChunk.class));
    }

    // -----------------------------------------------------------------------
    // 3. summary counts are correct
    // -----------------------------------------------------------------------

    @Test
    void indexAllArticles_returnsSummaryWithCorrectCounts() {
        KnowledgeArticle a1 = article(1L, "Conveyor", "CONVEYOR", "content");
        KnowledgeArticle a2 = article(2L, "Sensor", "SENSOR", "content");
        KnowledgeArticle a3 = article(3L, "Safety", "SAFETY", "content");
        when(articleRepository.findAll()).thenReturn(List.of(a1, a2, a3));
        when(chunkingService.chunk(a1)).thenReturn(List.of(chunk(1L, 0), chunk(1L, 1)));
        when(chunkingService.chunk(a2)).thenReturn(List.of(chunk(2L, 0)));
        when(chunkingService.chunk(a3)).thenReturn(List.of(chunk(3L, 0), chunk(3L, 1), chunk(3L, 2)));

        IndexingSummaryResponse summary = service.indexAllArticles();

        assertThat(summary.getArticlesIndexed()).isEqualTo(3);
        assertThat(summary.getChunksCreated()).isEqualTo(6);
    }

    @Test
    void indexAllArticles_returnsZeroCounts_whenNoArticlesExist() {
        when(articleRepository.findAll()).thenReturn(List.of());

        IndexingSummaryResponse summary = service.indexAllArticles();

        assertThat(summary.getArticlesIndexed()).isEqualTo(0);
        assertThat(summary.getChunksCreated()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // 4. embedding is called once per chunk
    // -----------------------------------------------------------------------

    @Test
    void indexAllArticles_callsEmbedding_oncePerChunk() {
        KnowledgeArticle article = article(1L, "Motor", "MOTOR", "content");
        when(articleRepository.findAll()).thenReturn(List.of(article));
        when(chunkingService.chunk(article)).thenReturn(List.of(
                new ArticleChunk(1L, 0, "chunk text 0", "Motor", "MOTOR"),
                new ArticleChunk(1L, 1, "chunk text 1", "Motor", "MOTOR")));

        service.indexAllArticles();

        verify(embeddingService).embed("chunk text 0");
        verify(embeddingService).embed("chunk text 1");
        verify(embeddingService, times(2)).embed(anyString());
    }

    // -----------------------------------------------------------------------
    // 5. embedding vector is stored via vectorChunkDao
    // -----------------------------------------------------------------------

    @Test
    void indexAllArticles_storesEmbedding_viaVectorChunkDao() {
        float[] vector = {0.1f, 0.5f, 0.9f};
        when(embeddingService.embed(anyString())).thenReturn(vector);

        KnowledgeArticle article = article(1L, "Motor", "MOTOR", "content");
        when(articleRepository.findAll()).thenReturn(List.of(article));
        when(chunkingService.chunk(article)).thenReturn(
                List.of(new ArticleChunk(1L, 0, "chunk text", "Motor", "MOTOR")));

        service.indexAllArticles();

        verify(vectorChunkDao).updateEmbedding(eq(99L), eq(vector));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private KnowledgeArticle article(Long id, String title, String category, String content) {
        KnowledgeArticle a = new KnowledgeArticle();
        setId(a, id);
        a.setTitle(title);
        a.setCategory(category);
        a.setContent(content);
        return a;
    }

    private ArticleChunk chunk(Long articleId, int index) {
        return new ArticleChunk(articleId, index, "text " + index, "Title", "CAT");
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}