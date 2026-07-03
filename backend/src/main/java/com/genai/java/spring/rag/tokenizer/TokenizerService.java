package com.genai.java.spring.rag.tokenizer;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.huggingface.tokenizers.Encoding;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TokenizerService {

    private static final Logger log = LoggerFactory.getLogger(TokenizerService.class);
    private static final String TOKENIZER_RESOURCE_DIR = "tokenizer/all-MiniLM-L6-v2";

    private final HuggingFaceTokenizer tokenizer;

    public TokenizerService() {
        try {
            Path localDir = extractTokenizerFilesToTempDir();
            this.tokenizer = HuggingFaceTokenizer.newInstance(localDir);
            log.info("Loaded local HuggingFace tokenizer from '{}'", localDir);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load bundled HuggingFace tokenizer files from classpath:"
                            + TOKENIZER_RESOURCE_DIR, e);
        }
    }

    private Path extractTokenizerFilesToTempDir() throws IOException {
        Path tempDir = Files.createTempDirectory("hf-tokenizer-");
        for (String fileName : new String[]{
                "tokenizer.json", "tokenizer_config.json", "vocab.txt", "special_tokens_map.json"
        }) {
            ClassPathResource resource = new ClassPathResource(TOKENIZER_RESOURCE_DIR + "/" + fileName);
            if (!resource.exists()) {
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, tempDir.resolve(fileName));
            }
        }
        return tempDir;
    }

    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Encoding encoding = tokenizer.encode(text);
        return encoding.getIds().length;
    }

    @PreDestroy
    public void close() {
        if (tokenizer != null) {
            tokenizer.close();
        }
    }
}