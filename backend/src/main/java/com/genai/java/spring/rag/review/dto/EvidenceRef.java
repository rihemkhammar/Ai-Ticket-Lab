package com.genai.java.spring.rag.review.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EvidenceRef implements java.io.Serializable {

    private String sourceRef;
    private String articleTitle;
}