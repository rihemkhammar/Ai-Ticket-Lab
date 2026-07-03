package com.genai.java.spring.rag.retrieval.dto;

public class EvidenceChunkResponse {

    private String sourceRef;
    private Long articleId;
    private String articleTitle;
    private String category;
    private int chunkIndex;
    private String text;
    private String expandedText;
    private Double similarityScore;

    public static EvidenceChunkResponse of(Long chunkArticleId, int chunkIndex, String text,
                                           String articleTitle, String category, Double similarityScore) {
        EvidenceChunkResponse dto = new EvidenceChunkResponse();
        dto.articleId = chunkArticleId;
        dto.chunkIndex = chunkIndex;
        dto.text = text;
        dto.expandedText = text;
        dto.articleTitle = articleTitle;
        dto.category = category;
        dto.similarityScore = similarityScore;
        dto.sourceRef = "article:" + chunkArticleId + "#chunk:" + chunkIndex;
        return dto;
    }

    public String getSourceRef()                   { return sourceRef; }
    public void setSourceRef(String v)              { this.sourceRef = v; }

    public Long getArticleId()                      { return articleId; }
    public void setArticleId(Long v)                { this.articleId = v; }

    public String getArticleTitle()                 { return articleTitle; }
    public void setArticleTitle(String v)           { this.articleTitle = v; }

    public String getCategory()                     { return category; }
    public void setCategory(String v)               { this.category = v; }

    public int getChunkIndex()                      { return chunkIndex; }
    public void setChunkIndex(int v)                { this.chunkIndex = v; }

    public String getText()                          { return text; }
    public void setText(String v)                     { this.text = v; }

    public String getExpandedText()                   { return expandedText; }
    public void setExpandedText(String v)              { this.expandedText = v; }

    public Double getSimilarityScore()                { return similarityScore; }
    public void setSimilarityScore(Double v)          { this.similarityScore = v; }
}