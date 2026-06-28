package com.genai.java.spring.knowledge;

public class KnowledgeArticleNotFoundException extends RuntimeException {

    public KnowledgeArticleNotFoundException(Long articleId) {
        super("Knowledge article not found: " + articleId);
    }
}