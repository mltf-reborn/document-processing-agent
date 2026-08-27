package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.config.GeminiAgentProperties;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationResponse;
import com.bagusxmahendra.mltf.document_processing_agent.prompt.SelfiePromptProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SelfieValidationAgentServiceTest {

    private GeminiAgentProperties properties;
    private GcsStorageService gcsStorageService;
    private SelfiePromptProvider promptProvider;
    private SelfieValidationAgentService service;

    @BeforeEach
    void setUp() {
        properties = new GeminiAgentProperties();
        properties.setModel("gemini-3.5-flash-lite");
        properties.setTemperature(0.1f);

        gcsStorageService = new GcsStorageService();
        promptProvider = new SelfiePromptProvider();

        service = new SelfieValidationAgentService(properties, gcsStorageService, promptProvider);
    }

    @Test
    void testValidateSelfie_NullOrEmptyParameters_ReturnsError() {
        // Null request
        StepVerifier.create(service.validateSelfie(null))
                .expectError(IllegalArgumentException.class)
                .verify();

        // Missing idDocumentUrl
        SelfieValidationRequest missingIdReq = new SelfieValidationRequest("", "gs://bucket/selfie.jpg");
        StepVerifier.create(service.validateSelfie(missingIdReq))
                .expectError(IllegalArgumentException.class)
                .verify();

        // Missing selfieUrl
        SelfieValidationRequest missingSelfieReq = new SelfieValidationRequest("gs://bucket/id.png", "");
        StepVerifier.create(service.validateSelfie(missingSelfieReq))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testParseAndBuildResponse_IdenticalMatch() throws Exception {
        String mockModelJson = """
                {
                  "isIdentical": true,
                  "confidenceScore": 96.8,
                  "matchStatus": "MATCH",
                  "explanation": "Biometric facial comparison confirms that the person in the selfie is identical to the individual in the photo ID.",
                  "facialComparisonDetails": {
                    "faceDetectedInId": true,
                    "faceDetectedInSelfie": true,
                    "facialLandmarksMatch": true,
                    "matchingFeatures": [
                      "Identical jawline and chin contour",
                      "Consistent interpupillary distance and eye slant",
                      "Matching nose bridge and nostril width"
                    ],
                    "discrepantFeatures": [],
                    "livenessCheck": {
                      "isLive": true,
                      "spoofRiskLevel": "LOW",
                      "findings": "Authentic skin texture, natural shadows, no screen bezel detected"
                    },
                    "riskLevel": "LOW",
                    "recommendation": "APPROVE"
                  }
                }
                """;

        Method parseMethod = SelfieValidationAgentService.class.getDeclaredMethod(
                "parseAndBuildResponse", String.class, String.class, String.class, String.class, String.class, long.class, Instant.class
        );
        parseMethod.setAccessible(true);

        SelfieValidationResponse response = (SelfieValidationResponse) parseMethod.invoke(
                service,
                mockModelJson,
                "gs://bucket/id.png",
                "image/png",
                "gs://bucket/selfie.jpg",
                "image/jpeg",
                System.currentTimeMillis(),
                Instant.now()
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getIsIdentical());
        assertEquals(96.8, response.getConfidenceScore());
        assertEquals(96.8, response.getConfidentScore());
        assertEquals("MATCH", response.getMatchStatus());
        assertTrue(response.getExplanation().contains("identical"));
        assertEquals("gs://bucket/id.png", response.getIdDocumentUrl());
        assertEquals("gs://bucket/selfie.jpg", response.getSelfieUrl());

        assertNotNull(response.getFacialComparisonDetails());
        assertTrue(response.getFacialComparisonDetails().isFaceDetectedInId());
        assertTrue(response.getFacialComparisonDetails().isFaceDetectedInSelfie());
        assertTrue(response.getFacialComparisonDetails().isFacialLandmarksMatch());
        assertEquals("APPROVE", response.getFacialComparisonDetails().getRecommendation());
        assertEquals(3, response.getFacialComparisonDetails().getMatchingFeatures().size());

        assertNotNull(response.getMetadata());
        assertEquals("gemini-3.5-flash-lite", response.getMetadata().getModel());
    }

    @Test
    void testParseAndBuildResponse_NonMatchWithNormalizedDecimalConfidence() throws Exception {
        String mockMismatchJson = """
                ```json
                {
                  "isIdentical": false,
                  "confidenceScore": 0.15,
                  "matchStatus": "NO_MATCH",
                  "explaination": "The facial bone structure, jawline, and nose shape differ significantly between the ID photo and the selfie.",
                  "facialComparisonDetails": {
                    "faceDetectedInId": true,
                    "faceDetectedInSelfie": true,
                    "facialLandmarksMatch": false,
                    "matchingFeatures": [],
                    "discrepantFeatures": [
                      "Jawline angle is angular in ID but rounded in selfie",
                      "Nose bridge is significantly narrower in ID photo"
                    ],
                    "livenessCheck": {
                      "isLive": true,
                      "spoofRiskLevel": "LOW",
                      "findings": "Selfie appears authentic but depicts a different person"
                    },
                    "riskLevel": "HIGH",
                    "recommendation": "REJECT"
                  }
                }
                ```
                """;

        Method parseMethod = SelfieValidationAgentService.class.getDeclaredMethod(
                "parseAndBuildResponse", String.class, String.class, String.class, String.class, String.class, long.class, Instant.class
        );
        parseMethod.setAccessible(true);

        SelfieValidationResponse response = (SelfieValidationResponse) parseMethod.invoke(
                service,
                mockMismatchJson,
                "gs://bucket/id.png",
                "image/png",
                "gs://bucket/selfie.jpg",
                "image/jpeg",
                System.currentTimeMillis(),
                Instant.now()
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertFalse(response.getIsIdentical());
        // 0.15 decimal converted to 15.0%
        assertEquals(15.0, response.getConfidenceScore());
        assertEquals("NO_MATCH", response.getMatchStatus());
        assertTrue(response.getExplanation().contains("differ significantly"));
        assertNotNull(response.getFacialComparisonDetails());
        assertFalse(response.getFacialComparisonDetails().isFacialLandmarksMatch());
        assertEquals("REJECT", response.getFacialComparisonDetails().getRecommendation());
        assertEquals(2, response.getFacialComparisonDetails().getDiscrepantFeatures().size());
    }

    @Test
    void testSanitizeJson_RemovesMarkdownBlocks() throws Exception {
        Method sanitizeMethod = SelfieValidationAgentService.class.getDeclaredMethod("sanitizeJson", String.class);
        sanitizeMethod.setAccessible(true);

        assertEquals("{\"test\": 1}", sanitizeMethod.invoke(service, "```json\n{\"test\": 1}\n```"));
        assertEquals("{\"test\": 2}", sanitizeMethod.invoke(service, "```{\"test\": 2}```"));
        assertEquals("{}", sanitizeMethod.invoke(service, (String) null));
    }
}
