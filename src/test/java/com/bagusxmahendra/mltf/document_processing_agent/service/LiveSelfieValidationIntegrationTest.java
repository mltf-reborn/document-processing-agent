package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LiveSelfieValidationIntegrationTest {

    @Autowired
    private SelfieValidationAgentService service;

    @Test
    void testLiveSelfieValidation() throws Exception {
        SelfieValidationRequest request = new SelfieValidationRequest(
                "gs://mltf-bucket/KYC-REV-2026-2941/document/Driver_License.png",
                "gs://mltf-bucket/KYC-REV-2026-2941/selfie/selfie.jpg"
        );

        System.out.println("=== Starting Live Selfie Validation for " + request.getIdDocumentUrl() + " and " + request.getSelfieUrl() + " ===");
        SelfieValidationResponse response = service.validateSelfie(request)
                .block(Duration.ofSeconds(90));

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        String jsonOutput = mapper.writeValueAsString(response);
        System.out.println("=== LIVE SELFIE RESPONSE OUTPUT ===");
        System.out.println(jsonOutput);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getIsIdentical());
        assertNotNull(response.getConfidenceScore());
        assertNotNull(response.getExplanation());
        assertNotNull(response.getFacialComparisonDetails());
    }
}
