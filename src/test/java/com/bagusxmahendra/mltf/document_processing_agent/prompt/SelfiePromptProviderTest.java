package com.bagusxmahendra.mltf.document_processing_agent.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SelfiePromptProviderTest {

    @Autowired
    private SelfiePromptProvider promptProvider;

    @Test
    void testSystemPromptLoadedSuccessfully() {
        assertNotNull(promptProvider, "SelfiePromptProvider should be injected");
        String systemPrompt = promptProvider.getSystemPrompt();
        assertNotNull(systemPrompt, "System prompt should not be null");
        assertFalse(systemPrompt.isBlank(), "System prompt should not be blank");

        // Verify key directives are present in the external prompt file
        assertTrue(systemPrompt.contains("FACE DETECTION & QUALITY EVALUATION"), "Prompt must contain face detection directive");
        assertTrue(systemPrompt.contains("BIOMETRIC FACIAL LANDMARK COMPARISON"), "Prompt must contain facial landmark comparison directive");
        assertTrue(systemPrompt.contains("ANTI-SPOOFING & LIVENESS INSPECTION"), "Prompt must contain liveness and anti-spoofing directive");
        assertTrue(systemPrompt.contains("isIdentical"), "Prompt must contain isIdentical schema field");
        assertTrue(systemPrompt.contains("confidenceScore"), "Prompt must contain confidenceScore schema field");
        assertTrue(systemPrompt.contains("DO NOT perform"), "Prompt must specify not to perform document info extraction");
        assertTrue(systemPrompt.contains("gemini-3.5-flash-lite"), "Prompt must reference model");
    }

    @Test
    void testBuildUserPromptWithCustomInstructions() {
        String promptWithCustom = promptProvider.buildUserPrompt("Focus on eye shape and nose contour");
        assertTrue(promptWithCustom.contains("Focus on eye shape and nose contour"));
        assertTrue(promptWithCustom.contains("Document 1 (Photo ID)"));
        assertTrue(promptWithCustom.contains("Document 2 (Selfie)"));
        assertTrue(promptWithCustom.contains("confidence score"));
    }
}
