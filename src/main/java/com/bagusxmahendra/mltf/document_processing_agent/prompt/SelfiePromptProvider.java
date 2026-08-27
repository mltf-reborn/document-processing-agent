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
 * Loads and manages system instructions and prompt templates for selfie validation.
 */
@Component
public class SelfiePromptProvider {

    private static final Logger log = LoggerFactory.getLogger(SelfiePromptProvider.class);

    @Value("classpath:prompts/selfie-validation-system-prompt.txt")
    private Resource systemPromptResource;

    private volatile String systemPrompt;

    @PostConstruct
    public void loadPrompt() {
        try {
            Resource res = this.systemPromptResource != null ? this.systemPromptResource :
                    new ClassPathResource("prompts/selfie-validation-system-prompt.txt");
            this.systemPrompt = res.getContentAsString(StandardCharsets.UTF_8);
            log.info("Successfully loaded selfie validation system prompt from external file ({} characters)", this.systemPrompt.length());
        } catch (IOException e) {
            log.error("Failed to load selfie validation prompt from resource", e);
            throw new IllegalStateException("Could not load selfie-validation-system-prompt.txt from classpath", e);
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
        StringBuilder sb = new StringBuilder("Please perform biometric facial validation ONLY between Document 1 (Photo ID) and Document 2 (Selfie). Determine if the selfie is the identical person as shown on the photo ID, check liveness, and return the confidence score and forensic explanation. Do NOT perform text or field extraction from the document.");
        if (customInstructions != null && !customInstructions.trim().isEmpty()) {
            sb.append("\n\nAdditional instructions:\n").append(customInstructions.trim());
        }
        return sb.toString();
    }
}
