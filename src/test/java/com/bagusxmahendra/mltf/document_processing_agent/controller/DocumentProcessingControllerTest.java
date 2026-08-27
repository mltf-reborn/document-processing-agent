package com.bagusxmahendra.mltf.document_processing_agent.controller;

import com.bagusxmahendra.mltf.document_processing_agent.dto.*;
import com.bagusxmahendra.mltf.document_processing_agent.exception.GlobalExceptionHandler;
import com.bagusxmahendra.mltf.document_processing_agent.service.DocumentProcessingAgentService;
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

class DocumentProcessingControllerTest {

    private WebTestClient webTestClient;
    private DocumentProcessingAgentService documentProcessingAgentService;
    private DocumentProcessingResponse mockResponse;

    @BeforeEach
    void setUp() {
        documentProcessingAgentService = Mockito.mock(DocumentProcessingAgentService.class);
        DocumentProcessingController controller = new DocumentProcessingController(documentProcessingAgentService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

        webTestClient = WebTestClient.bindToController(controller)
                .controllerAdvice(exceptionHandler)
                .build();

        mockResponse = new DocumentProcessingResponse();
        mockResponse.setStatus("SUCCESS");
        mockResponse.setMessage("Document processed successfully");
        mockResponse.setGcsUrl("gs://test-bucket/invoice-001.pdf");
        mockResponse.setDetectedDocumentType("INVOICE");

        DocumentScores scores = new DocumentScores(
                98.5,
                100.0,
                96.2,
                "Originality: 100.0% (Pristine pixels), Confidence: 96.2% (Crisp text)"
        );
        mockResponse.setScores(scores);

        PixelLevelCheckResult pixelCheck = new PixelLevelCheckResult(
                false,
                "NONE",
                0.0,
                "Pixel analysis confirmed zero font antialiasing discrepancies or tampering.",
                List.of()
        );
        mockResponse.setPixelLevelCheck(pixelCheck);

        mockResponse.setExtractedFields(Map.of(
                "invoiceNumber", "INV-2026-999",
                "totalAmount", "$4,500.00",
                "vendorName", "Cloud Services Inc."
        ));

        mockResponse.setFieldDetails(List.of(
                new DocumentFieldDetail("invoiceNumber", "INV-2026-999", 0.99, false, "Uniform font"),
                new DocumentFieldDetail("totalAmount", "$4,500.00", 0.98, false, "Consistent baseline")
        ));

        mockResponse.setMetadata(new ProcessingMetadata(
                "gemini-3.5-flash-lite",
                "Google ADK (Agent Development Kit)",
                "application/pdf",
                Instant.now(),
                450
        ));
    }

    @Test
    void testProcessDocumentPost_Success() {
        when(documentProcessingAgentService.processDocument(any(DocumentProcessingRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        String requestJson = """
                {
                    "gcs_url": "gs://test-bucket/invoice-001.pdf",
                    "mime_type": "application/pdf"
                }
                """;

        webTestClient.post()
                .uri("/api/v1/doc/processing")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.gcsUrl").isEqualTo("gs://test-bucket/invoice-001.pdf")
                .jsonPath("$.detectedDocumentType").isEqualTo("INVOICE")
                .jsonPath("$.scores.documentScore").isEqualTo(98.5)
                .jsonPath("$.scores.originalityScore").isEqualTo(100.0)
                .jsonPath("$.scores.confidenceScore").isEqualTo(96.2)
                .jsonPath("$.pixelLevelCheck.isTampered").isEqualTo(false)
                .jsonPath("$.extractedFields.invoiceNumber").isEqualTo("INV-2026-999")
                .jsonPath("$.extractedFields.totalAmount").isEqualTo("$4,500.00")
                .jsonPath("$.metadata.model").isEqualTo("gemini-3.5-flash-lite")
                .jsonPath("$.metadata.agentFramework").isEqualTo("Google ADK (Agent Development Kit)");
    }

    @Test
    void testProcessDocumentGet_Success() {
        when(documentProcessingAgentService.processDocument(any(DocumentProcessingRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.get()
                .uri("/api/v1/doc/processing?gcsUrl=gs://test-bucket/invoice-001.pdf")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.scores.documentScore").isEqualTo(98.5);
    }

    @Test
    void testProcessDocumentGet_MissingUrl_ReturnsBadRequest() {
        webTestClient.get()
                .uri("/api/v1/doc/processing")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat(msg.toString()).contains("required"));
    }

    @Test
    void testProcessDocumentPost_QueryParamsFallback() {
        when(documentProcessingAgentService.processDocument(any(DocumentProcessingRequest.class)))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri("/api/v1/doc/processing?gcsUrl=gs://test-bucket/invoice-001.pdf")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.detectedDocumentType").isEqualTo("INVOICE");
    }

    @Test
    void testProcessDocument_ServiceException_ReturnsUnprocessableEntity() {
        when(documentProcessingAgentService.processDocument(any(DocumentProcessingRequest.class)))
                .thenReturn(Mono.error(new com.bagusxmahendra.mltf.document_processing_agent.exception.DocumentProcessingException("gs://invalid/path.pdf", "GCS Object not found")));

        webTestClient.post()
                .uri("/api/v1/doc/processing?gcsUrl=gs://invalid/path.pdf")
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody()
                .jsonPath("$.status").isEqualTo("FAILED")
                .jsonPath("$.message").value(msg -> org.assertj.core.api.Assertions.assertThat(msg.toString()).contains("GCS Object not found"));
    }
}
