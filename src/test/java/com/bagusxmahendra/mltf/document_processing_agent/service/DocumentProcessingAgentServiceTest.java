package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.config.GeminiAgentProperties;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingResponse;
import com.bagusxmahendra.mltf.document_processing_agent.prompt.DocumentPromptProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.time.Instant;

import com.bagusxmahendra.mltf.document_processing_agent.tools.DocumentForensicTools;
import static org.junit.jupiter.api.Assertions.*;

class DocumentProcessingAgentServiceTest {

    private GeminiAgentProperties properties;
    private GcsStorageService gcsStorageService;
    private DocumentPromptProvider promptProvider;
    private DocumentForensicTools forensicTools;
    private DocumentProcessingAgentService service;

    @BeforeEach
    void setUp() {
        properties = new GeminiAgentProperties();
        properties.setModel("gemini-3.5-flash-lite");
        properties.setTemperature(0.1f);

        gcsStorageService = new GcsStorageService();
        promptProvider = new DocumentPromptProvider();
        forensicTools = new DocumentForensicTools();

        service = new DocumentProcessingAgentService(properties, gcsStorageService, promptProvider, forensicTools);
    }

    @Test
    void testProcessDocument_NullOrEmptyGcsUrl_ReturnsError() {
        DocumentProcessingRequest emptyReq = new DocumentProcessingRequest("");
        StepVerifier.create(service.processDocument(emptyReq))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(service.processDocument(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void testParseAndBuildResponse_AuthenticDocument() throws Exception {
        String mockModelJson = """
                {
                  "detectedDocumentType": "INVOICE",
                  "originalityScore": 100.0,
                  "confidenceScore": 96.0,
                  "documentScore": 98.4,
                  "scoringBreakdown": "Originality: 100%, Confidence: 96%",
                  "pixelLevelCheck": {
                    "isTampered": false,
                    "tamperingRiskLevel": "NONE",
                    "tamperingConfidence": 0.0,
                    "findings": "All characters have uniform antialiasing and consistent pixel noise.",
                    "anomalies": []
                  },
                  "extractedFields": {
                    "invoiceNumber": "INV-10023",
                    "total": "$1,200.00"
                  },
                  "fieldDetails": [
                    {
                      "key": "invoiceNumber",
                      "value": "INV-10023",
                      "confidence": 0.99,
                      "isSuspicious": false,
                      "notes": "Consistent font"
                    }
                  ]
                }
                """;

        Method parseMethod = DocumentProcessingAgentService.class.getDeclaredMethod(
                "parseAndBuildResponse", String.class, String.class, String.class, long.class, Instant.class
        );
        parseMethod.setAccessible(true);

        DocumentProcessingResponse response = (DocumentProcessingResponse) parseMethod.invoke(
                service, mockModelJson, "gs://bucket/inv.pdf", "application/pdf", System.currentTimeMillis(), Instant.now()
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("INVOICE", response.getDetectedDocumentType());
        assertEquals(98.4, response.getScores().getDocumentScore());
        assertEquals(100.0, response.getScores().getOriginalityScore());
        assertEquals(96.0, response.getScores().getConfidenceScore());
        assertFalse(response.getPixelLevelCheck().isTampered());
        assertEquals("INV-10023", response.getExtractedFields().get("invoiceNumber"));
        assertEquals("$1,200.00", response.getExtractedFields().get("total"));
    }

    @Test
    void testParseAndBuildResponse_TamperedDocument() throws Exception {
        String mockTamperedJson = """
                ```json
                {
                  "detectedDocumentType": "BANK_STATEMENT",
                  "originalityScore": 25.0,
                  "confidenceScore": 80.0,
                  "documentScore": 47.0,
                  "scoringBreakdown": "Originality: 25% due to pixel tampering; Confidence: 80%",
                  "pixelLevelCheck": {
                    "isTampered": true,
                    "tamperingRiskLevel": "HIGH",
                    "tamperingConfidence": 95.0,
                    "findings": "Pixel-level inspection revealed font antialiasing discrepancies in the account balance digits.",
                    "anomalies": [
                      {
                        "targetField": "accountBalance",
                        "anomalyType": "FONT_MISMATCH",
                        "severity": "HIGH",
                        "description": "Digit '9' has different pixel smoothing and kerning compared to adjacent digits '500'."
                      }
                    ]
                  },
                  "extractedFields": {
                    "accountNumber": "123456789",
                    "accountBalance": "$9,500.00"
                  },
                  "fieldDetails": [
                    {
                      "key": "accountBalance",
                      "value": "$9,500.00",
                      "confidence": 0.50,
                      "isSuspicious": true,
                      "notes": "Digit 9 was spliced"
                    }
                  ]
                }
                ```
                """;

        Method parseMethod = DocumentProcessingAgentService.class.getDeclaredMethod(
                "parseAndBuildResponse", String.class, String.class, String.class, long.class, Instant.class
        );
        parseMethod.setAccessible(true);

        DocumentProcessingResponse response = (DocumentProcessingResponse) parseMethod.invoke(
                service, mockTamperedJson, "gs://bucket/statement.png", "image/png", System.currentTimeMillis(), Instant.now()
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertTrue(response.getPixelLevelCheck().isTampered());
        assertEquals("HIGH", response.getPixelLevelCheck().getTamperingRiskLevel());
        assertEquals(1, response.getPixelLevelCheck().getAnomalies().size());
        assertEquals("FONT_MISMATCH", response.getPixelLevelCheck().getAnomalies().get(0).getAnomalyType());
        assertTrue(response.getScores().getDocumentScore() < 50.0);
    }

    @Test
    void testParseAndBuildResponse_BankStatementTransactions() throws Exception {
        String mockBankStatementJson = """
                {
                  "detectedDocumentType": "BANK_STATEMENT",
                  "originalityScore": 99.0,
                  "confidenceScore": 98.0,
                  "documentScore": 98.6,
                  "scoringBreakdown": "Originality: 99%, Confidence: 98%",
                  "pixelLevelCheck": {
                    "isTampered": false,
                    "tamperingRiskLevel": "NONE",
                    "tamperingConfidence": 0.0,
                    "findings": "Consistent pixel rendering across all transaction rows.",
                    "anomalies": []
                  },
                  "extractedFields": {
                    "bankName": "Bank ABC",
                    "accountNumber": "1234567890",
                    "transaction1": "20250801#transaction desctiption1#4500",
                    "transaction2": "20250801#transaction desctiption2#300"
                  },
                  "fieldDetails": [
                    {
                      "key": "transaction1",
                      "value": "20250801#transaction desctiption1#4500",
                      "confidence": 0.99,
                      "isSuspicious": false,
                      "notes": "Transaction row 1"
                    },
                    {
                      "key": "transaction2",
                      "value": "20250801#transaction desctiption2#300",
                      "confidence": 0.98,
                      "isSuspicious": false,
                      "notes": "Transaction row 2"
                    }
                  ]
                }
                """;

        Method parseMethod = DocumentProcessingAgentService.class.getDeclaredMethod(
                "parseAndBuildResponse", String.class, String.class, String.class, long.class, Instant.class
        );
        parseMethod.setAccessible(true);

        DocumentProcessingResponse response = (DocumentProcessingResponse) parseMethod.invoke(
                service, mockBankStatementJson, "gs://bucket/bank_statement.pdf", "application/pdf", System.currentTimeMillis(), Instant.now()
        );

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals("BANK_STATEMENT", response.getDetectedDocumentType());
        assertEquals("Bank ABC", response.getExtractedFields().get("bankName"));
        assertEquals("1234567890", response.getExtractedFields().get("accountNumber"));
        assertEquals("20250801#transaction desctiption1#4500", response.getExtractedFields().get("transaction1"));
        assertEquals("20250801#transaction desctiption2#300", response.getExtractedFields().get("transaction2"));
        assertEquals(2, response.getFieldDetails().size());
    }

    @Test
    void testInit_InitializesAgentAndRunner() throws Exception {
        properties.setApiKey("test-api-key");
        service.init();

        Method initMethod = DocumentProcessingAgentService.class.getDeclaredMethod("initAdkAgent");
        initMethod.setAccessible(true);
        initMethod.invoke(service);
        // Repeated invocation is idempotent
        initMethod.invoke(service);
    }

    @Test
    void testSanitizeJson_RemovesMarkdownBlocks() throws Exception {
        Method sanitizeMethod = DocumentProcessingAgentService.class.getDeclaredMethod("sanitizeJson", String.class);
        sanitizeMethod.setAccessible(true);

        assertEquals("{\"test\": 1}", sanitizeMethod.invoke(service, "```json\n{\"test\": 1}\n```"));
        assertEquals("{\"test\": 2}", sanitizeMethod.invoke(service, "```{\"test\": 2}```"));
        assertEquals("{}", sanitizeMethod.invoke(service, (String) null));
    }

    @Test
    void testCalculateCombinedScore() throws Exception {
        Method scoreMethod = DocumentProcessingAgentService.class.getDeclaredMethod("calculateCombinedScore", double.class, double.class, boolean.class);
        scoreMethod.setAccessible(true);

        double scoreAuthentic = (Double) scoreMethod.invoke(service, 100.0, 100.0, false);
        assertEquals(100.0, scoreAuthentic);

        double scoreTampered = (Double) scoreMethod.invoke(service, 100.0, 100.0, true);
        assertEquals(58.0, scoreTampered); // (30.0 * 0.6) + (100.0 * 0.4) = 18 + 40 = 58.0
    }
}
