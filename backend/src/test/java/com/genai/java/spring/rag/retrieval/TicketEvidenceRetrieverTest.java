package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import com.genai.java.spring.rag.rerank.RerankerService;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketEvidenceRetrieverTest {

    @Mock private HybridSearchService hybridSearchService;
    @Mock private RerankerService rerankerService;
    @Mock private NeighborStitchingService neighborStitchingService;
    @InjectMocks private TicketEvidenceRetriever retriever;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retriever, "topK", 3);
        ReflectionTestUtils.setField(retriever, "fusedPoolSize", 10);
        // lenient(): this stub is not exercised by every test (e.g. the
        // "empty chunks" case never calls stitch()) — that's expected here.
        // Nouvelle signature : stitch(row, usedIndexes) au lieu de stitch(row).
        // Défensif : protège contre un argument null (ex: si un test re-stub
        // via when(...).thenAnswer(...), ce qui déclenche un vrai appel sur
        // le mock avec des arguments null avant que le nouveau stub existe).
        lenient().when(neighborStitchingService.stitch(any(), any())).thenAnswer(inv -> {
            VectorSearchRow row = inv.getArgument(0);
            return row == null ? null : row.text();
        });
    }

    private Ticket ticket(String title, String desc) {
        Ticket t = new Ticket(); t.setTitle(title); t.setDescription(desc); return t;
    }

    private VectorSearchRow row(long articleId, int chunkIndex, String category, double distance) {
        return new VectorSearchRow(articleId * 10, articleId, chunkIndex,
                "chunk text", "Article " + articleId, category, distance);
    }

    /** Simulates the pipeline: hybrid search returns candidates, reranker returns them capped at topK. */
    private void stubPipeline(List<VectorSearchRow> hybridResults) {
        when(hybridSearchService.search(any(), anyInt())).thenReturn(hybridResults);
        when(rerankerService.rerank(any(), any(), anyInt())).thenAnswer(inv -> {
            List<VectorSearchRow> candidates = inv.getArgument(1);
            int topK = inv.getArgument(2);
            return candidates.size() <= topK ? candidates : candidates.subList(0, topK);
        });
    }

    @Test @DisplayName("retourne les chunks mappés depuis le pipeline hybrid+rerank")
    void retrieve_returnsChunks() {
        stubPipeline(List.of(
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
        stubPipeline(List.of());
        assertThat(retriever.retrieve(ticket("Unknown", ""))).isEmpty();
    }

    @Test @DisplayName("respecte la limite topK")
    void retrieve_respectsTopK() {
        stubPipeline(List.of(
                row(1L, 0, "MOTOR", 0.1), row(2L, 0, "PUMP", 0.2),
                row(3L, 0, "SENSOR", 0.3), row(4L, 0, "CONVEYOR", 0.4),
                row(5L, 0, "MOTOR", 0.5)
        ));
        assertThat(retriever.retrieve(ticket("Generic", ""))).hasSizeLessThanOrEqualTo(3);
    }

    @Test @DisplayName("similarityScore est inversement proportionnel à la distance")
    void retrieve_similarityScoreIsInverse() {
        stubPipeline(List.of(
                row(1L, 0, "MOTOR", 0.1),
                row(2L, 0, "MOTOR", 1.0)
        ));
        List<EvidenceChunkResponse> result = retriever.retrieve(ticket("Motor", "issue"));
        assertThat(result.get(0).getSimilarityScore()).isGreaterThan(result.get(1).getSimilarityScore());
    }

    // ── Nouveau test : prouve la correction du doublon (S3-F05 fix) ────────────

    @Test @DisplayName("le même Set usedIndexes est partagé entre tous les appels stitch() d'un même ticket")
    void retrieve_sharesUsedIndexesAcrossStitchCalls() {
        stubPipeline(List.of(
                row(1L, 2, "MOTOR", 0.1), // article 1, chunk voisin de chunk 3
                row(1L, 3, "MOTOR", 0.2)  // article 1, chunk voisin de chunk 2
        ));

        // On capture le Set passé à chaque appel pour vérifier qu'il s'agit
        // bien de la MÊME instance (donc partagée) entre les deux chunks.
        //
        // IMPORTANT : on utilise doAnswer(...).when(mock).stitch(...) et non
        // when(mock.stitch(...)).thenAnswer(...). La deuxième forme exécute
        // un VRAI appel sur le mock pour construire le stub ; comme un stub
        // matchant (any(), any()) existe déjà depuis setUp(), cet appel réel
        // (avec des arguments null car any() ne fournit pas de vraie valeur)
        // était intercepté par l'ANCIEN stub, causant un NullPointerException
        // avant même que le nouveau stub ne soit enregistré. doAnswer(...)
        // évite ce problème car il ne déclenche jamais d'appel réel sur le mock.
        Set<?>[] capturedSets = new Set<?>[2];
        int[] callCount = {0};
        doAnswer(inv -> {
            capturedSets[callCount[0]++] = inv.getArgument(1);
            return ((VectorSearchRow) inv.getArgument(0)).text();
        }).when(neighborStitchingService).stitch(any(), any());

        retriever.retrieve(ticket("Motor", "issue"));

        assertThat(capturedSets[0]).isSameAs(capturedSets[1]);
    }
}