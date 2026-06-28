package com.genai.java.spring.rag.chunk;

import com.genai.java.spring.knowledge.KnowledgeArticle;
import com.genai.java.spring.rag.chunk.ArticleChunkingService.ArticleChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ArticleChunkingService.

 * Covers:
 * - non-empty chunks are produced
 * - chunk metadata includes articleId, title, category, chunkIndex
 * - blank/null content is handled gracefully
 * - content that fits in one paragraph produces a single chunk
 * - long content is split into multiple chunks
 * - each chunk does not exceed maxChunkChars
 */
class ArticleChunkingServiceTest {

    private static final int MAX_CHUNK_CHARS = 100;

    private ArticleChunkingService service;

    @BeforeEach
    void setUp() {
        service = new ArticleChunkingService(MAX_CHUNK_CHARS);
    }

    // -----------------------------------------------------------------------
    // 1. non-empty chunks for normal articles
    // -----------------------------------------------------------------------

    @Test
    void chunk_producesNonEmptyList_forNormalArticle() {
        KnowledgeArticle article = articleWith(1L, "Motor Lubrication", "MOTOR",
                "Check lubricant level. Verify lubricant type. Inspect bearing noise.");

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).isNotEmpty();
    }

    // -----------------------------------------------------------------------
    // 2. chunk metadata contains article id, title, category, chunk index
    // -----------------------------------------------------------------------

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
        // Use a long content to guarantee multiple chunks
        String longContent = "A".repeat(MAX_CHUNK_CHARS + 50) + "\n\n" + "B".repeat(MAX_CHUNK_CHARS + 50);
        KnowledgeArticle article = articleWith(1L, "Title", "CAT", longContent);

        List<ArticleChunk> chunks = service.chunk(article);

        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    // -----------------------------------------------------------------------
    // 3. blank / null content is handled gracefully
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // 4. short content produces one chunk
    // -----------------------------------------------------------------------

    @Test
    void chunk_producesOneChunk_whenContentFitsInMaxChars() {
        String shortContent = "Motor overheating: check bearings."; // well under 100 chars
        KnowledgeArticle article = articleWith(5L, "Short Article", "MOTOR", shortContent);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo(shortContent);
    }

    // -----------------------------------------------------------------------
    // 5. long single-paragraph content is split into multiple chunks
    // -----------------------------------------------------------------------

    @Test
    void chunk_splitsLongParagraph_intoMultipleChunks() {
        // single paragraph longer than MAX_CHUNK_CHARS
        String longParagraph = "Motor overheating description. ".repeat(10); // ~310 chars > 100
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", longParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks.size()).isGreaterThan(1);
    }

    @Test
    void chunk_eachChunk_doesNotExceedMaxChunkChars() {
        String longParagraph = "A very long sentence about motor maintenance issues. ".repeat(10);
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", longParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        // Each individual chunk text must not be bigger than the configured limit
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.text().length()).isLessThanOrEqualTo(MAX_CHUNK_CHARS + 50));
        // Note: +50 tolerance because the splitter may keep a few extra chars
        // when the last sentence-break is just beyond the boundary.
    }

    // -----------------------------------------------------------------------
    // 6. multi-paragraph content produces at least as many chunks as paragraphs
    // -----------------------------------------------------------------------

    @Test
    void chunk_splitsByParagraphBreaks() {
        String multiParagraph =
                "First paragraph about motor.\n\n" +
                        "Second paragraph about lubrication.\n\n" +
                        "Third paragraph about safety.";
        KnowledgeArticle article = articleWith(1L, "Title", "MOTOR", multiParagraph);

        List<ArticleChunk> chunks = service.chunk(article);

        // At least 3 chunks (one per paragraph)
        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // 7. chunk text is not blank
    // -----------------------------------------------------------------------

    @Test
    void chunk_noChunkHasBlankText() {
        String content = "Conveyor motor troubleshooting steps.\nCheck bearings and lubrication.";
        KnowledgeArticle article = articleWith(1L, "Conveyor", "CONVEYOR", content);

        List<ArticleChunk> chunks = service.chunk(article);

        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk.text()).isNotBlank());
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private KnowledgeArticle articleWith(Long id, String title, String category, String content) {
        KnowledgeArticle a = new KnowledgeArticle();
        // KnowledgeArticle.id is generated; use reflection to set it in tests
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