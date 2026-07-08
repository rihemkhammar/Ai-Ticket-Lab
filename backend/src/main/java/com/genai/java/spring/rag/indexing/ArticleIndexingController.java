package com.genai.java.spring.rag.indexing;


import com.genai.java.spring.rag.indexing.dto.IndexingSummaryResponse;
import org.springframework.http.ResponseEntity;

import com.genai.java.spring.rag.indexing.dto.ArticleIndexStatusResponse;
import com.genai.java.spring.rag.indexing.dto.IndexingSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleIndexingController {

    private final ArticleIndexingService indexingService;

    public ArticleIndexingController(ArticleIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/index")
    public ResponseEntity<IndexingSummaryResponse> indexArticles() {
        return ResponseEntity.ok(indexingService.indexAllArticles());
    }

    //  real DB-backed count, safe to call on every page load
    // (dashboard, articles page) instead of relying on the last indexing
    // response, which is lost on navigation.
    @GetMapping("/index/status")
    public ResponseEntity<ArticleIndexStatusResponse> getIndexStatus() {
        return ResponseEntity.ok(indexingService.getStatus());
    }
}