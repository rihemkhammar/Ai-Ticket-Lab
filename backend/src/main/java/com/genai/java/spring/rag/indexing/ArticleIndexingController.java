package com.genai.java.spring.rag.indexing;

import com.genai.java.spring.rag.indexing.dto.IndexingSummaryResponse;
import org.springframework.http.ResponseEntity;
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
}