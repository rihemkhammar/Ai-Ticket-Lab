package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import com.genai.java.spring.rag.embedding.EmbeddingService;
import com.genai.java.spring.rag.retrieval.dto.EvidenceChunkResponse;
import com.genai.java.spring.ticket.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketEvidenceRetrieverTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private VectorChunkDao vectorChunkDao;
    @InjectMocks private TicketEvidenceRetriever retriever;

    @BeforeEach
    void setUp() { ReflectionTestUtils.setField(retriever, "topK", 3); }

    private Ticket ticket(String title, String desc) {
        Ticket t = new Ticket(); t.setTitle(title); t.setDescription(desc); return t;
    }

    private VectorSearchRow row(long articleId, int chunkIndex, String category, double distance) {
        return new VectorSearchRow(articleId * 10, articleId, chunkIndex,
                "chunk text", "Article " + articleId, category, distance);
    }

    @Test @DisplayName("retourne les chunks mappés depuis le DAO")
    void retrieve_returnsChunks() {
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorChunkDao.search(any(), anyInt())).thenReturn(List.of(
                row(1L, 0, "MOTOR", 0.1),
                row(2L, 1, "PUMP", 0.3)
        ));
        List<EvidenceChunkResponse> result = retriever.retrieve(ticket("Motor overheating", "Fan not working"));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSourceRef()).isEqualTo("article:1#chunk:0");
        assertThat(result.get(1).getSourceRef()).isEqualTo("article:2#chunk:1");
    }

    @Test @DisplayName("retourne une liste vide si aucun chunk trouvé")
    void retrieve_emptyWhenNoChunks() {
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorChunkDao.search(any(), anyInt())).thenReturn(List.of());
        assertThat(retriever.retrieve(ticket("Unknown", ""))).isEmpty();
    }

    @Test @DisplayName("respecte la limite topK")
    void retrieve_respectsTopK() {
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorChunkDao.search(any(), anyInt())).thenReturn(List.of(
                row(1L, 0, "MOTOR", 0.1), row(2L, 0, "PUMP", 0.2),
                row(3L, 0, "SENSOR", 0.3), row(4L, 0, "CONVEYOR", 0.4),
                row(5L, 0, "MOTOR", 0.5)
        ));
        assertThat(retriever.retrieve(ticket("Generic", ""))).hasSizeLessThanOrEqualTo(3);
    }

    @Test @DisplayName("similarityScore est inversement proportionnel à la distance")
    void retrieve_similarityScoreIsInverse() {
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorChunkDao.search(any(), anyInt())).thenReturn(List.of(
                row(1L, 0, "MOTOR", 0.1),
                row(2L, 0, "MOTOR", 1.0)
        ));
        List<EvidenceChunkResponse> result = retriever.retrieve(ticket("Motor", "issue"));
        assertThat(result.get(0).getSimilarityScore()).isGreaterThan(result.get(1).getSimilarityScore());
    }
}