package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.config.GeminiAgentProperties;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingResponse;
import com.bagusxmahendra.mltf.document_processing_agent.prompt.DocumentPromptProvider;
import com.bagusxmahendra.mltf.document_processing_agent.tools.DocumentForensicTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LiveDocumentProcessingIntegrationTest {

    @Autowired
    private DocumentProcessingAgentService service;

    @Test
    void testLiveDriverLicenseProcessing() throws Exception {
        DocumentProcessingRequest request = new DocumentProcessingRequest(
                "gs://mltf-bucket/KYC-REV-2026-2941/document/Driver_License.png"
        );

        System.out.println("=== Starting Live Document Processing for " + request.getGcsUrl() + " ===");
        DocumentProcessingResponse response = service.processDocument(request)
                .block(Duration.ofSeconds(60));

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        String jsonOutput = mapper.writeValueAsString(response);
        System.out.println("=== LIVE RESPONSE OUTPUT ===");
        System.out.println(jsonOutput);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getDetectedDocumentType());
        assertNotNull(response.getScores());
        assertNotNull(response.getPixelLevelCheck());
        assertNotNull(response.getExtractedFields());
    }
}
