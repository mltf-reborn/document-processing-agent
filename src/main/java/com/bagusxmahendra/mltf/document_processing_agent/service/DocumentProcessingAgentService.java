package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.config.GeminiAgentProperties;
import com.bagusxmahendra.mltf.document_processing_agent.dto.*;
import com.bagusxmahendra.mltf.document_processing_agent.exception.DocumentProcessingException;
import com.bagusxmahendra.mltf.document_processing_agent.prompt.DocumentPromptProvider;
import com.bagusxmahendra.mltf.document_processing_agent.tools.DocumentForensicTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.models.Gemini;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.SessionKey;
import com.google.adk.tools.FunctionTool;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.FileData;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Service orchestrating the Google ADK Agent powered by gemini-3.5-flash-lite
 * for forensic document analysis, pixel checking, and key-value extraction.
 */
@Service
public class DocumentProcessingAgentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingAgentService.class);

    private final GeminiAgentProperties properties;
    private final GcsStorageService gcsStorageService;
    private final DocumentPromptProvider promptProvider;
    private final DocumentForensicTools forensicTools;
    private final ObjectMapper objectMapper;

    private Client genAiClient;
    private LlmAgent adkAgent;
    private InMemoryRunner adkRunner;

    public DocumentProcessingAgentService(
            GeminiAgentProperties properties,
            GcsStorageService gcsStorageService,
            DocumentPromptProvider promptProvider,
            DocumentForensicTools forensicTools) {
        this.properties = properties;
        this.gcsStorageService = gcsStorageService;
        this.promptProvider = promptProvider;
        this.forensicTools = forensicTools;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        try {
            initAdkAgent();
            log.info("Google ADK Document Processing Agent initialized with model: {}", properties.getModel());
        } catch (Exception e) {
            log.warn("Google ADK Document Processing Agent deferred initialization: {}", e.getMessage());
        }
    }

    private synchronized void initAdkAgent() {
        if (this.adkAgent != null && this.adkRunner != null) {
            return;
        }

        Client.Builder clientBuilder = Client.builder();
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            clientBuilder.apiKey(properties.getApiKey());
        }
        if (properties.isUseVertexAi()) {
            clientBuilder.vertexAI(true);
            if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
                clientBuilder.project(properties.getProjectId());
            }
            if (properties.getLocation() != null && !properties.getLocation().isBlank()) {
                clientBuilder.location(properties.getLocation());
            }
        }

        this.genAiClient = clientBuilder.build();

        Gemini gemini = Gemini.builder()
                .modelName(properties.getModel())
                .apiClient(this.genAiClient)
                .build();

        GenerateContentConfig contentConfig = GenerateContentConfig.builder()
                .temperature(properties.getTemperature())
                .build();

        List<Object> tools = new ArrayList<>();
        if (this.forensicTools != null) {
            try {
                tools.add(FunctionTool.create(forensicTools, "validateMathCalculations"));
                tools.add(FunctionTool.create(forensicTools, "verifyChecksum"));
                tools.add(FunctionTool.create(forensicTools, "validateDateSequence"));
            } catch (Exception e) {
                log.warn("Could not register ADK forensic tools: {}", e.getMessage());
            }
        }

        LlmAgent.Builder agentBuilder = LlmAgent.builder()
                .name("document-processing-agent")
                .description("Forensic document processing agent for pixel integrity checking, dynamic key-value extraction, and scoring")
                .instruction(promptProvider.getSystemPrompt())
                .model(gemini)
                .generateContentConfig(contentConfig);

        if (!tools.isEmpty()) {
            agentBuilder.tools(tools);
        }

        this.adkAgent = agentBuilder.build();

        this.adkRunner = new InMemoryRunner(this.adkAgent, "document-processing-app");
    }

    /**
     * Reactively processes the document from the GCS URL using Google ADK with gemini-3.5-flash-lite.
     */
    public Mono<DocumentProcessingResponse> processDocument(DocumentProcessingRequest request) {
        long startTime = System.currentTimeMillis();
        Instant processedAt = Instant.now();

        if (request == null || request.getGcsUrl() == null || request.getGcsUrl().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("gcsUrl parameter is required and must not be empty"));
        }

        String rawUrl = request.getGcsUrl().trim();
        String normalizedGcsUri = gcsStorageService.normalizeGcsUri(rawUrl);
        String detectedMimeType = gcsStorageService.detectMimeType(normalizedGcsUri, request.getMimeType());

        log.info("Starting reactive document processing for GCS URL: [{}], mimeType: [{}]", normalizedGcsUri, detectedMimeType);

        return buildContentPayload(normalizedGcsUri, detectedMimeType, request.getCustomPrompt())
                .flatMap(content -> executeAdkAgent(content, normalizedGcsUri))
                .map(rawJson -> parseAndBuildResponse(rawJson, normalizedGcsUri, detectedMimeType, startTime, processedAt))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .doOnError(err -> log.error("Error during document processing for [{}]: {}", normalizedGcsUri, err.getMessage(), err));
    }

    /**
     * Builds multimodal Content using either direct GCS URI (via FileData) or downloaded blob bytes (via inlineData).
     */
    private Mono<Content> buildContentPayload(String gcsUri, String mimeType, String customPrompt) {
        String userPromptText = promptProvider.buildUserPrompt(customPrompt);

        // Try downloading blob bytes for resilient inline multimodal analysis
        return gcsStorageService.downloadBlobBytes(gcsUri)
                .map(bytes -> {
                    log.info("Successfully downloaded {} bytes from GCS for URI: [{}], mimeType: [{}]", bytes.length, gcsUri, mimeType);
                    Part imagePart = Part.fromBytes(bytes, mimeType);
                    Part textPart = Part.fromText(userPromptText);
                    return Content.builder()
                            .role("user")
                            .parts(List.of(imagePart, textPart))
                            .build();
                })
                .onErrorResume(err -> {
                    log.warn("Direct GCS byte download not available ({}), referencing GCS URI directly: {}", err.getMessage(), gcsUri);
                    Part filePart = Part.fromUri(gcsUri, mimeType);
                    Part textPart = Part.fromText(userPromptText);
                    return Mono.just(Content.builder()
                            .role("user")
                            .parts(List.of(filePart, textPart))
                            .build());
                });
    }

    /**
     * Executes the Google ADK Agent via InMemoryRunner and collects the reactive Event stream into a JSON response.
     */
    private Mono<String> executeAdkAgent(Content content, String gcsUri) {
        return Mono.defer(() -> {
            try {
                initAdkAgent();
            } catch (Exception e) {
                return Mono.error(new DocumentProcessingException(gcsUri, "Failed to initialize Google ADK Agent: " + e.getMessage(), e));
            }

            String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
            String sessionId = "sess-" + UUID.randomUUID().toString();
            SessionKey sessionKey = new SessionKey(adkRunner.appName(), userId, sessionId);

            // Create session in ADK InMemoryRunner session service
            return Mono.<com.google.adk.sessions.Session>create(sink -> {
                adkRunner.sessionService().createSession(sessionKey)
                        .subscribe(sink::success, sink::error);
            })
            .flatMap(session -> {
                log.info("Created ADK session: {} for user: {}", session.id(), userId);
                // Run agent asynchronously and adapt RxJava3 Flowable to Project Reactor Flux
                return Flux.from(adkRunner.runAsync(sessionKey, content))
                        .collectList()
                        .map(this::extractTextFromEvents);
            })
            .subscribeOn(Schedulers.boundedElastic());
        });
    }

    /**
     * Extracts and combines text content from ADK Events, prioritizing finalResponse and assistant output.
     */
    private String extractTextFromEvents(List<Event> events) {
        // Look for the finalResponse event first
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (event.finalResponse() && event.content().isPresent()) {
                String text = extractTextFromContent(event.content().get());
                if (!text.isBlank()) {
                    log.debug("Found finalResponse event text: {}", text);
                    return text.trim();
                }
            }
        }

        // Look for the last event with content
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get(i);
            if (event.content().isPresent()) {
                String text = extractTextFromContent(event.content().get());
                if (!text.isBlank()) {
                    return text.trim();
                }
            }
            if (event.stringifyContent() != null && !event.stringifyContent().isBlank()) {
                return event.stringifyContent().trim();
            }
        }

        // Aggregation fallback
        StringBuilder fullResponse = new StringBuilder();
        for (Event event : events) {
            if (event.content().isPresent()) {
                fullResponse.append(extractTextFromContent(event.content().get()));
            }
        }

        String rawOutput = fullResponse.toString().trim();
        log.debug("Raw output from Google ADK Agent: {}", rawOutput);
        return rawOutput;
    }

    private String extractTextFromContent(Content content) {
        StringBuilder sb = new StringBuilder();
        if (content.parts().isPresent()) {
            for (Part part : content.parts().get()) {
                part.text().ifPresent(sb::append);
            }
        }
        return sb.toString();
    }

    /**
     * Cleans, parses, and enriches the model JSON output into DocumentProcessingResponse.
     */
    private DocumentProcessingResponse parseAndBuildResponse(String rawJson, String gcsUrl, String detectedMimeType, long startTime, Instant processedAt) {
        long durationMs = System.currentTimeMillis() - startTime;
        String cleanJson = sanitizeJson(rawJson);

        try {
            JsonNode root = objectMapper.readTree(cleanJson);

            DocumentProcessingResponse response = new DocumentProcessingResponse();
            response.setStatus("SUCCESS");
            response.setMessage("Document processed successfully");
            response.setGcsUrl(gcsUrl);

            // Document Type
            if (root.has("detectedDocumentType")) {
                response.setDetectedDocumentType(root.get("detectedDocumentType").asText());
            } else if (root.has("documentType")) {
                response.setDetectedDocumentType(root.get("documentType").asText());
            } else {
                response.setDetectedDocumentType("DOCUMENT");
            }

            // Pixel Level Check
            PixelLevelCheckResult pixelCheck = new PixelLevelCheckResult();
            if (root.has("pixelLevelCheck")) {
                JsonNode pxNode = root.get("pixelLevelCheck");
                pixelCheck = objectMapper.treeToValue(pxNode, PixelLevelCheckResult.class);
            } else {
                pixelCheck.setTampered(false);
                pixelCheck.setTamperingRiskLevel("NONE");
                pixelCheck.setTamperingConfidence(0.0);
                pixelCheck.setFindings("No pixel tampering detected.");
                pixelCheck.setAnomalies(Collections.emptyList());
            }
            response.setPixelLevelCheck(pixelCheck);

            // Scores
            double originalityScore = root.has("originalityScore") ? root.get("originalityScore").asDouble(100.0) : 100.0;
            double confidenceScore = root.has("confidenceScore") ? root.get("confidenceScore").asDouble(100.0) : 100.0;
            double documentScore = root.has("documentScore") ? root.get("documentScore").asDouble() :
                    calculateCombinedScore(originalityScore, confidenceScore, pixelCheck.isTampered());

            String scoringBreakdown = root.has("scoringBreakdown") ? root.get("scoringBreakdown").asText() :
                    String.format("Originality: %.1f%%, Confidence: %.1f%%, Document Score: %.1f%%", originalityScore, confidenceScore, documentScore);

            DocumentScores scores = new DocumentScores(
                    roundToTwoDecimals(documentScore),
                    roundToTwoDecimals(originalityScore),
                    roundToTwoDecimals(confidenceScore),
                    scoringBreakdown
            );
            response.setScores(scores);

            // Extracted Fields (Dynamic Key-Pair)
            Map<String, Object> extractedFields = new LinkedHashMap<>();
            if (root.has("extractedFields") && root.get("extractedFields").isObject()) {
                extractedFields = objectMapper.convertValue(root.get("extractedFields"), new TypeReference<Map<String, Object>>() {});
            }
            response.setExtractedFields(extractedFields);

            // Field Details
            List<DocumentFieldDetail> fieldDetails = new ArrayList<>();
            if (root.has("fieldDetails") && root.get("fieldDetails").isArray()) {
                fieldDetails = objectMapper.convertValue(root.get("fieldDetails"), new TypeReference<List<DocumentFieldDetail>>() {});
            }
            response.setFieldDetails(fieldDetails);

            // Metadata
            ProcessingMetadata metadata = new ProcessingMetadata(
                    properties.getModel(),
                    "Google ADK (Agent Development Kit)",
                    detectedMimeType,
                    processedAt,
                    durationMs
            );
            response.setMetadata(metadata);

            return response;

        } catch (Exception e) {
            log.error("Failed to parse ADK Agent response JSON: {}", cleanJson, e);
            throw new DocumentProcessingException(gcsUrl, "Failed to parse model output into structured document response: " + e.getMessage(), e);
        }
    }

    private double calculateCombinedScore(double originalityScore, double confidenceScore, boolean isTampered) {
        if (isTampered) {
            originalityScore = Math.min(originalityScore, 30.0);
        }
        double combined = (originalityScore * 0.6) + (confidenceScore * 0.4);
        return Math.max(0.0, Math.min(100.0, combined));
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String sanitizeJson(String text) {
        if (text == null) {
            return "{}";
        }
        String s = text.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
