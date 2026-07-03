package com.genai.java.spring.knowledge.dto;

import com.genai.java.spring.knowledge.KnowledgeArticle;

public class KnowledgeArticleResponse {

    private Long id;
    private String title;
    private String category;
    private String content;

    public static KnowledgeArticleResponse from(KnowledgeArticle article) {
        KnowledgeArticleResponse dto = new KnowledgeArticleResponse();
        dto.id = article.getId();
        dto.title = article.getTitle();
        dto.category = article.getCategory();
        dto.content = article.getContent();
        return dto;
    }

    public Long getId()                   { return id; }
    public void setId(Long v)             { this.id = v; }

    public String getTitle()              { return title; }
    public void setTitle(String v)        { this.title = v; }

    public String getCategory()           { return category; }
    public void setCategory(String v)     { this.category = v; }

    public String getContent()            { return content; }
    public void setContent(String v)      { this.content = v; }
}