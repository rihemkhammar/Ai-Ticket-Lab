package com.genai.java.spring.rag.retrieval;

import com.genai.java.spring.rag.chunk.VectorChunkDao;
import com.genai.java.spring.rag.chunk.VectorChunkDao.VectorSearchRow;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NeighborStitchingService {

    private final VectorChunkDao vectorChunkDao;

    public NeighborStitchingService(VectorChunkDao vectorChunkDao) {
        this.vectorChunkDao = vectorChunkDao;
    }

    // usedIndexes = les (articleId, chunkIndex) déjà envoyés dans un autre chunk du même batch
    public String stitch(VectorSearchRow chunk, Set<String> usedIndexes) {
        List<VectorSearchRow> neighbors = vectorChunkDao.findNeighbors(chunk.articleId(), chunk.chunkIndex());

        StringBuilder result = new StringBuilder();
        neighbors.stream()
                .sorted(Comparator.comparingInt(VectorSearchRow::chunkIndex))
                .forEach(n -> {
                    String key = n.articleId() + ":" + n.chunkIndex();
                    if (usedIndexes.contains(key)) {
                        return; // déjà inclus par un autre chunk du top-K → on saute
                    }
                    usedIndexes.add(key);
                    if (result.length() > 0) result.append(" ");
                    result.append(n.text());
                });

        return result.length() > 0 ? result.toString() : chunk.text();
    }
}