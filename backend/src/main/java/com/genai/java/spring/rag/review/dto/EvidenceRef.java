package com.genai.java.spring.rag.review.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EvidenceRef {

    private String sourceRef;
    private String articleTitle;
}