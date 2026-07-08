package com.genai.java.spring.rag.indexing.dto;

/**
 * Live snapshot of indexing state, read directly from the database
 * (article count + semantic_chunk count) instead of the ephemeral
 * result of the last "index now" call. Used by the dashboard and the
 * knowledge-articles page so the chunk count survives navigation and
 * page reloads.
 */
public class ArticleIndexStatusResponse {

    private long articlesTotal;
    private long chunksTotal;

    public ArticleIndexStatusResponse() {}

    public ArticleIndexStatusResponse(long articlesTotal, long chunksTotal) {
        this.articlesTotal = articlesTotal;
        this.chunksTotal = chunksTotal;
    }

    public long getArticlesTotal()          { return articlesTotal; }
    public void setArticlesTotal(long v)    { this.articlesTotal = v; }

    public long getChunksTotal()            { return chunksTotal; }
    public void setChunksTotal(long v)      { this.chunksTotal = v; }
}
