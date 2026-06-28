package com.genai.java.spring.knowledge;

import com.genai.java.spring.knowledge.dto.KnowledgeArticleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleRepository repository;

    public KnowledgeArticleController(KnowledgeArticleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<KnowledgeArticleResponse> list() {
        return repository.findAll().stream()
                .map(KnowledgeArticleResponse::from)
                .toList();
    }

    @GetMapping("/{articleId}")
    public ResponseEntity<KnowledgeArticleResponse> getOne(@PathVariable Long articleId) {
        KnowledgeArticle article = repository.findById(articleId)
                .orElseThrow(() -> new KnowledgeArticleNotFoundException(articleId));
        return ResponseEntity.ok(KnowledgeArticleResponse.from(article));
    }
}