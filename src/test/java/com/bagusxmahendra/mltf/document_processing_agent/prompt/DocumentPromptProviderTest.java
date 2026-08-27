package com.bagusxmahendra.mltf.document_processing_agent.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DocumentPromptProviderTest {

    @Autowired
    private DocumentPromptProvider promptProvider;

    @Test
    void testSystemPromptLoadedSuccessfully() {
        assertNotNull(promptProvider, "Prompt provider should be injected");
        String systemPrompt = promptProvider.getSystemPrompt();
        assertNotNull(systemPrompt, "System prompt should not be null");
        assertFalse(systemPrompt.isBlank(), "System prompt should not be blank");

        // Verify key directives are present in the external prompt file
        assertTrue(systemPrompt.contains("PIXEL-LEVEL TAMPERING"), "Prompt must contain pixel-level tampering directive");
        assertTrue(systemPrompt.contains("FLEXIBLE KEY-VALUE EXTRACTION"), "Prompt must contain key-value extraction directive");
        assertTrue(systemPrompt.contains("DOCUMENT SCORE CALCULATION"), "Prompt must contain score calculation directive");
        assertTrue(systemPrompt.contains("gemini-3.5-flash-lite"), "Prompt must reference model");
    }

    @Test
    void testBuildUserPromptWithCustomInstructions() {
        String promptWithCustom = promptProvider.buildUserPrompt("Focus on invoice table items");
        assertTrue(promptWithCustom.contains("Focus on invoice table items"));
        assertTrue(promptWithCustom.contains("pixel-level tampering"));
    }
}
