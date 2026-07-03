package com.genai.java.spring.rag.chunk;

import com.genai.java.spring.knowledge.KnowledgeArticle;
import com.genai.java.spring.rag.tokenizer.TokenizerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleChunkingService {

    private final int maxChunkTokens;
    private final TokenizerService tokenizerService;

    public ArticleChunkingService(
            @Value("${app.rag.max-chunk-tokens:220}") int maxChunkTokens,
            TokenizerService tokenizerService) {
        this.maxChunkTokens = maxChunkTokens;
        this.tokenizerService = tokenizerService;
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

    /**
     * Splits a paragraph into pieces that each stay under maxChunkTokens,
     * using the model's REAL tokenizer instead of a character-count guess.
     * This guarantees the embedding model never silently truncates a chunk.
     */
    private List<String> splitByLength(String paragraph) {
        if (tokenizerService.countTokens(paragraph) <= maxChunkTokens) {
            return List.of(paragraph);
        }

        // Split into sentences first, so we don't cut mid-sentence when possible.
        List<String> sentences = splitIntoSentences(paragraph);
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String candidate = current.isEmpty() ? sentence : current + " " + sentence;
            if (tokenizerService.countTokens(candidate) <= maxChunkTokens) {
                current = new StringBuilder(candidate);
                continue;
            }

            if (!current.isEmpty()) {
                pieces.add(current.toString());
                current = new StringBuilder();
            }

            if (tokenizerService.countTokens(sentence) <= maxChunkTokens) {
                current = new StringBuilder(sentence);
            } else {
                // A single sentence alone exceeds the limit: hard-split it word by word.
                pieces.addAll(splitLongSentenceByWords(sentence));
            }
        }

        if (!current.isEmpty()) {
            pieces.add(current.toString());
        }
        return pieces;
    }

    private List<String> splitIntoSentences(String paragraph) {
        String[] raw = paragraph.split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String s : raw) {
            if (!s.isBlank()) sentences.add(s.trim());
        }
        return sentences.isEmpty() ? List.of(paragraph) : sentences;
    }

    private List<String> splitLongSentenceByWords(String sentence) {
        String[] words = sentence.split("\\s+");
        List<String> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (tokenizerService.countTokens(candidate) <= maxChunkTokens) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) pieces.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) pieces.add(current.toString());
        return pieces;
    }

    public record ArticleChunk(
            Long articleId,
            int chunkIndex,
            String text,
            String articleTitle,
            String category
    ) {}
}