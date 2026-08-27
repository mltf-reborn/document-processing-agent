package com.bagusxmahendra.mltf.document_processing_agent.controller;

import com.bagusxmahendra.mltf.document_processing_agent.dto.*;
import com.bagusxmahendra.mltf.document_processing_agent.exception.GlobalExceptionHandler;
import com.bagusxmahendra.mltf.document_processing_agent.exception.SelfieValidationException;
import com.bagusxmahendra.mltf.document_processing_agent.service.SelfieValidationAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SelfieValidationControllerTest {

    private WebTestClient webTestClient;
    private SelfieValidationAgentService selfieValidationAgentService;
    private SelfieValidationResponse mockResponse;

    @BeforeEach
    void setUp() {
        selfieValidationAgentService = Mockito.mock(SelfieValidationAgentService.class);
        SelfieValidationController controller = new SelfieValidationController(selfieValidationAgentService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(exceptionHandler)
                .build();

        mockResponse = new SelfieValidationResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("Selfie validation completed successfully");
        mockResponse.setIsIdentical(true);
        mockResponse.setConfidenceScore(97.5);
        mockResponse.setMatchStatus("MATCH");
        mockResponse.setExplanation("Biometric facial features match with high confidence.");
        mockResponse.setIdDocumentUrl("gs://test-bucket/kyc/id_card.png");
        mockResponse.setSelfieUrl("gs://test-bucket/kyc/selfie.jpg");

        FacialComparisonDetails details = new FacialComparisonDetails(
                true,
                true,
                true,
                List.of("Identical jawline", "Matching nose bridge"),
                List.of(),
                new LivenessCheckResult(true, "NONE", "Natural skin texture and lighting"),
                "LOW",
                "APPROVE"
        );
        mockResponse.setFacialComparisonDetails(details);
        mockResponse.setMetadata(new ProcessingMetadata(
                "gemini-3.5-flash-lite",
                "Google ADK (Agent Development Kit)",
                "id: image/png, selfie: image/jpeg",
                Instant.now(),
                520
        ));
    }

    @Test
    void testValidateSelfiePost_Success() {
        when(selfieValidationAgentService.validateSelfie(any(SelfieValidationRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        String requestJson = """
                {
                    "id_document_url": "gs://test-bucket/kyc/id_card.png",
                    "selfie_url": "gs://test-bucket/kyc/selfie.jpg"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/selfie/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.isIdentical").isEqualTo(true)
                .jsonPath("$.confidenceScore").isEqualTo(97.5)
                .jsonPath("$.matchStatus").isEqualTo("MATCH")
                .jsonPath("$.explanation").isEqualTo("Biometric facial features match with high confidence.")
                .jsonPath("$.idDocumentUrl").isEqualTo("gs://test-bucket/kyc/id_card.png")
                .jsonPath("$.selfieUrl").isEqualTo("gs://test-bucket/kyc/selfie.jpg")
                .jsonPath("$.facialComparisonDetails.faceDetectedInId").isEqualTo(true)
                .jsonPath("$.facialComparisonDetails.faceDetectedInSelfie").isEqualTo(true)
                .jsonPath("$.facialComparisonDetails.recommendation").isEqualTo("APPROVE")
                .jsonPath("$.metadata.model").isEqualTo("gemini-3.5-flash-lite");
    }

    @Test
    void testValidateSelfieGet_Success() {
        when(selfieValidationAgentService.validateSelfie(any(SelfieValidationRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.get()
                .uri("/api/v1/selfie/validation?idDocumentUrl=gs://test-bucket/kyc/id_card.png&selfieUrl=gs://test-bucket/kyc/selfie.jpg")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.isIdentical").isEqualTo(true)
                .jsonPath("$.confidenceScore").isEqualTo(97.5);
    }

    @Test
    void testValidateSelfieGet_MissingSelfieUrl_ReturnsBadRequest() {
        webTestClient.get()
                .uri("/api/v1/selfie/validation?idDocumentUrl=gs://test-bucket/kyc/id_card.png")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat(msg.toString()).contains("selfieUrl"));
    }

    @Test
    void testValidateSelfiePost_QueryParamsFallback() {
        when(selfieValidationAgentService.validateSelfie(any(SelfieValidationRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri("/api/v1/selfie/validate?id_document_url=gs://test-bucket/kyc/id_card.png&selfie_url=gs://test-bucket/kyc/selfie.jpg")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.isIdentical").isEqualTo(true);
    }

    @Test
    void testValidateSelfie_ServiceException_ReturnsUnprocessableEntity() {
        when(selfieValidationAgentService.validateSelfie(any(SelfieValidationRequest.class)))
                .thenReturn(Mono.error(new SelfieValidationException("gs://invalid/id.png", "gs://invalid/selfie.jpg", "GCS image not found")));

        webTestClient.post()
                .uri("/api/v1/selfie/validation?idDocumentUrl=gs://invalid/id.png&selfieUrl=gs://invalid/selfie.jpg")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat(msg.toString()).contains("GCS image not found"));
    }
}
