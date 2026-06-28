package com.genai.java.spring.rag.indexing.dto;

public class IndexingSummaryResponse {

    private int articlesIndexed;
    private int chunksCreated;

    public IndexingSummaryResponse() {}

    public IndexingSummaryResponse(int articlesIndexed, int chunksCreated) {
        this.articlesIndexed = articlesIndexed;
        this.chunksCreated = chunksCreated;
    }

    public int getArticlesIndexed()              { return articlesIndexed; }
    public void setArticlesIndexed(int v)        { this.articlesIndexed = v; }

    public int getChunksCreated()                { return chunksCreated; }
    public void setChunksCreated(int v)          { this.chunksCreated = v; }

}