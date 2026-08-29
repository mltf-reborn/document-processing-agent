package com.bagusxmahendra.mltf.document_processing_agent.prompt;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads and manages system instructions and prompt templates from external resource files.
 */
@Component
public class DocumentPromptProvider {

    private static final Logger log = LoggerFactory.getLogger(DocumentPromptProvider.class);

    @Value("classpath:prompts/document-processing-system-prompt.txt")
    private Resource systemPromptResource;

    private volatile String systemPrompt;

    @PostConstruct
    public void loadPrompt() {
        try {
            Resource res = this.systemPromptResource != null ? this.systemPromptResource :
                    new ClassPathResource("prompts/document-processing-system-prompt.txt");
            this.systemPrompt = res.getContentAsString(StandardCharsets.UTF_8);
            log.info("Successfully loaded document processing system prompt from external file ({} characters)", this.systemPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load prompt from resource", e);
            throw new IllegalStateException("Could not load document-processing-system-prompt.txt from classpath", e);
        }
    }

    public String getSystemPrompt() {
        if (systemPrompt == null) {
            loadPrompt();
        }
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String buildUserPrompt(String customInstructions) {
        StringBuilder sb = new StringBuilder("Please thoroughly analyze this document for pixel-level tampering, extract all dynamic key-value pairs visibly present in the document, and calculate authenticity/confidence scores.\n\nCRITICAL ANTI-HALLUCINATION: Strictly extract ONLY values that are visibly present in the document. Do NOT hallucinate, infer, guess, or fabricate any non-existent values or fields. When a value cannot be found or is not present, send NULL or an empty string \"\" (or omit it); do NOT put any other value or placeholder.");
        if (customInstructions != null && !customInstructions.trim().isEmpty()) {
            sb.append("\n\nAdditional user guidelines:\n").append(customInstructions.trim());
        }
        return sb.toString();
    }
}
