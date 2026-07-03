package com.genai.java.spring.rag.chunk;

import com.genai.java.spring.knowledge.KnowledgeArticle;
import com.genai.java.spring.rag.chunk.ArticleChunkingService.ArticleChunk;
import com.genai.java.spring.rag.tokenizer.TokenizerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleChunkingServiceTest {

    // Small token budget so short test strings can still trigger splitting.
    private static final int MAX_CHUNK_TOKENS = 20;

    private ArticleChunkingService service;
    private TokenizerService tokenizerService;

    @BeforeEach
    void setUp() {
        tokenizerService = new TokenizerService();
        service = new ArticleChunkingService(MAX_CHUNK_TOKENS, tokenizerService);
    }

    @Test
    void chunk_producesNonEmptyList_forNormalArticle() {
        KnowledgeArticle article = articleWith(1L, "Motor Lubrication", "MOTOR",
                "Check lubricant level. Verify lubricant type. Inspect bearing noise.");

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).isNotEmpty();
    }

    @Test
    void chunk_includesCorrectMetadata() {
        KnowledgeArticle article = articleWith(42L, "Sensor Calibration", "SENSOR",
                "Verify wiring. Check signal stability.");

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.articleId()).isEqualTo(42L);
            assertThat(chunk.articleTitle()).isEqualTo("Sensor Calibration");
            assertThat(chunk.category()).isEqualTo("SENSOR");
            assertThat(chunk.chunkIndex()).isGreaterThanOrEqualTo(0);
        });
    }

    @Test
    void chunk_indexesAreSequentialStartingAtZero() {
        String longContent = "Motor bearing lubrication check. ".repeat(20) + "\n\n"
                + "Sensor calibration and wiring inspection. ".repeat(20);
        KnowledgeArticle article = articleWith(1L, "Title", "CAT", longContent);

        List<ArticleChunk> chunks = service.chunk(article);

        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    @Test
    void chunk_returnsEmptyList_whenContentIsNull() {
        KnowledgeArticle article = articleWith(1L, "Title", "CAT", null);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_returnsEmptyList_whenContentIsBlank() {
        KnowledgeArticle article = articleWith(1L, "Title", "CAT", "   \n  ");

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).isEmpty();
    }

    @Test
    void chunk_producesOneChunk_whenContentFitsInMaxTokens() {
        String shortContent = "Motor overheating: check bearings.";
        KnowledgeArticle article = articleWith(5L, "Short Article", "MOTOR", shortContent);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo(shortContent);
    }

    @Test
    void chunk_splitsLongParagraph_intoMultipleChunks() {
        String longParagraph = "Motor overheating description. ".repeat(10);
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", longParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void chunk_eachChunk_doesNotExceedMaxChunkTokens() {
        String longParagraph = "A very long sentence about motor maintenance issues. ".repeat(10);
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", longParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).allSatisfy(chunk ->
                assertThat(tokenizerService.countTokens(chunk.text()))
                        .isLessThanOrEqualTo(MAX_CHUNK_TOKENS));
    }

    @Test
    void chunk_splitsByParagraphBreaks() {
        String multiParagraph =
                "First paragraph about motor.\n\n" +
                        "Second paragraph about lubrication.\n\n" +
                        "Third paragraph about safety.";
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", multiParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void chunk_noChunkHasBlankText() {
        String content = "Conveyor motor troubleshooting steps.\nCheck bearings and lubrication.";
        KnowledgeArticle article = articleWith(1L, "Conveyor", "CONVEYOR", content);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.text()).isNotBlank());
    }

    private KnowledgeArticle articleWith(Long id, String title, String category, String content) {
        KnowledgeArticle a = new KnowledgeArticle();
        try {
            var field = KnowledgeArticle.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(a, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        a.setTitle(title);
        a.setCategory(category);
        a.setContent(content);
        return a;
    }
}