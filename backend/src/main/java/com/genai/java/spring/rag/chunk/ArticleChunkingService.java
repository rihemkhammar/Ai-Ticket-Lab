package com.genai.java.spring.rag.chunk;

import com.genai.java.spring.knowledge.KnowledgeArticle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleChunkingService {

    private final int maxChunkChars;

    public ArticleChunkingService(
            @Value("${app.rag.max-chunk-chars:400}") int maxChunkChars) {
        this.maxChunkChars = maxChunkChars;
    }

    public List<ArticleChunk> chunk(KnowledgeArticle article) {
        List<ArticleChunk> chunks = new ArrayList<>();
        List<String> paragraphs = splitIntoParagraphs(article.getContent());
        int index = 0;
        for (String paragraph : paragraphs) {
            for (String piece : splitByLength(paragraph)) {
                String trimmed = piece.trim();
                if (trimmed.isEmpty()) continue;
                chunks.add(new ArticleChunk(
                        article.getId(),
                        index++,
                        trimmed,
                        article.getTitle(),
                        article.getCategory()
                ));
            }
        }
        if (chunks.isEmpty() && article.getContent() != null && !article.getContent().isBlank()) {
            chunks.add(new ArticleChunk(
                    article.getId(), 0, article.getContent().trim(),
                    article.getTitle(), article.getCategory()));
        }
        return chunks;
    }

    private List<String> splitIntoParagraphs(String content) {
        if (content == null) return List.of();
        String[] raw = content.split("\\r?\\n\\s*\\r?\\n|\\r?\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String p : raw) {
            if (!p.isBlank()) paragraphs.add(p.trim());
        }
        return paragraphs;
    }

    private List<String> splitByLength(String paragraph) {
        if (paragraph.length() <= maxChunkChars) {
            return List.of(paragraph);
        }
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + maxChunkChars, paragraph.length());
            if (end < paragraph.length()) {
                int lastBreak = lastIndexOfAny(paragraph, ". ", end, start);
                if (lastBreak > start) end = lastBreak + 1;
            }
            pieces.add(paragraph.substring(start, end));
            start = end;
        }
        return pieces;
    }

    private int lastIndexOfAny(String text, String delimiter, int beforeIndex, int afterIndex) {
        int idx = text.lastIndexOf(delimiter, beforeIndex);
        return idx > afterIndex ? idx : -1;
    }

    public record ArticleChunk(
            Long articleId,
            int chunkIndex,
            String text,
            String articleTitle,
            String category
    ) {}
}