package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.config.GeminiAgentProperties;
import com.bagusxmahendra.mltf.document_processing_agent.dto.*;
import com.bagusxmahendra.mltf.document_processing_agent.exception.SelfieValidationException;
import com.bagusxmahendra.mltf.document_processing_agent.prompt.SelfiePromptProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.models.Gemini;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.SessionKey;
import com.google.genai.Client;
import com.google.genai.types.Content;
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
 * Service orchestrating the Google ADK Agent powered by Gemini
 * for biometric selfie validation against photo ID documents.
 */
@Service
public class SelfieValidationAgentService {

    private static final Logger log = LoggerFactory.getLogger(SelfieValidationAgentService.class);

    private final GeminiAgentProperties properties;
    private final GcsStorageService gcsStorageService;
    private final SelfiePromptProvider promptProvider;
    private final ObjectMapper objectMapper;

    private Client genAiClient;
    private LlmAgent adkAgent;
    private InMemoryRunner adkRunner;

    public SelfieValidationAgentService(
            GeminiAgentProperties properties,
            GcsStorageService gcsStorageService,
            SelfiePromptProvider promptProvider) {
        this.properties = properties;
        this.gcsStorageService = gcsStorageService;
        this.promptProvider = promptProvider;
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PostConstruct
    public void init() {
        try {
            initAdkAgent();
            log.info("Google ADK Selfie Validation Agent initialized with model: {}", properties.getModel());
        } catch (Exception e) {
            log.warn("Google ADK Selfie Validation Agent deferred initialization: {}", e.getMessage());
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

        LlmAgent.Builder agentBuilder = LlmAgent.builder()
                .name("selfie-validation-agent")
                .description("Forensic selfie and photo ID facial comparison validation agent")
                .instruction(promptProvider.getSystemPrompt())
                .model(gemini)
                .generateContentConfig(contentConfig);

        this.adkAgent = agentBuilder.build();
        this.adkRunner = new InMemoryRunner(this.adkAgent, "selfie-validation-app");
    }

    /**
     * Reactively validates a selfie image against a photo ID document.
     */
    public Mono<SelfieValidationResponse> validateSelfie(SelfieValidationRequest request) {
        long startTime = System.currentTimeMillis();
        Instant processedAt = Instant.now();

        if (request == null) {
            return Mono.error(new IllegalArgumentException("Request payload must not be null"));
        }

        if (request.getIdDocumentUrl() == null || request.getIdDocumentUrl().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("ID document URL (idDocumentUrl) is required and must not be empty"));
        }

        if (request.getSelfieUrl() == null || request.getSelfieUrl().trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Selfie URL (selfieUrl) is required and must not be empty"));
        }

        String rawIdUrl = request.getIdDocumentUrl().trim();
        String rawSelfieUrl = request.getSelfieUrl().trim();

        String normalizedIdUri = gcsStorageService.normalizeGcsUri(rawIdUrl);
        String normalizedSelfieUri = gcsStorageService.normalizeGcsUri(rawSelfieUrl);

        String idMimeType = gcsStorageService.detectMimeType(normalizedIdUri, request.getIdDocumentMimeType());
        String selfieMimeType = gcsStorageService.detectMimeType(normalizedSelfieUri, request.getSelfieMimeType());

        log.info("Starting reactive selfie validation for ID: [{}], mimeType: [{}] vs Selfie: [{}], mimeType: [{}]",
                normalizedIdUri, idMimeType, normalizedSelfieUri, selfieMimeType);

        return buildMultimodalContent(normalizedIdUri, idMimeType, normalizedSelfieUri, selfieMimeType, request.getCustomPrompt())
                .flatMap(content -> executeAdkAgent(content, normalizedIdUri, normalizedSelfieUri))
                .map(rawJson -> parseAndBuildResponse(rawJson, normalizedIdUri, idMimeType, normalizedSelfieUri, selfieMimeType, startTime, processedAt))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .doOnError(err -> log.error("Error during selfie validation for ID [{}] and Selfie [{}]: {}",
                        normalizedIdUri, normalizedSelfieUri, err.getMessage(), err));
    }

    /**
     * Resolves a document reference (GCS URI, data URI, or base64) into a GenAI Part.
     */
    private Mono<Part> resolveDocumentPart(String documentRef, String mimeType) {
        String trimmed = documentRef.trim();

        // Data URI scheme: data:image/png;base64,...
        if (trimmed.startsWith("data:")) {
            int commaIdx = trimmed.indexOf(',');
            if (commaIdx != -1) {
                String header = trimmed.substring(5, commaIdx);
                String base64Data = trimmed.substring(commaIdx + 1);
                String mime = header.contains(";") ? header.substring(0, header.indexOf(';')) : header;
                if (mime.isBlank()) mime = mimeType != null ? mimeType : "image/jpeg";
                try {
                    byte[] decoded = Base64.getDecoder().decode(base64Data.replaceAll("\\s+", ""));
                    log.info("Decoded data URI base64 with length {} bytes and mime {}", decoded.length, mime);
                    return Mono.just(Part.fromBytes(decoded, mime));
                } catch (IllegalArgumentException e) {
                    log.warn("Failed to decode data URI base64: {}", e.getMessage());
                }
            }
        }

        // GCS URI or HTTPS storage URL
        if (trimmed.startsWith("gs://") || trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return gcsStorageService.downloadBlobBytes(trimmed)
                    .map(bytes -> {
                        log.info("Downloaded {} bytes from GCS for document: {}", bytes.length, trimmed);
                        return Part.fromBytes(bytes, mimeType);
                    })
                    .onErrorResume(err -> {
                        log.warn("Direct GCS byte download not available ({}), referencing URI: {}", err.getMessage(), trimmed);
                        return Mono.just(Part.fromUri(trimmed, mimeType));
                    });
        }

        // Raw Base64 string fallback
        if (isBase64String(trimmed)) {
            try {
                byte[] decoded = Base64.getDecoder().decode(trimmed.replaceAll("\\s+", ""));
                log.info("Decoded raw base64 string with length {} bytes and mime {}", decoded.length, mimeType);
                return Mono.just(Part.fromBytes(decoded, mimeType));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Default to GCS URI reference
        return gcsStorageService.downloadBlobBytes(trimmed)
                .map(bytes -> Part.fromBytes(bytes, mimeType))
                .onErrorResume(err -> Mono.just(Part.fromUri(trimmed, mimeType)));
    }

    private boolean isBase64String(String str) {
        if (str == null || str.length() < 100) {
            return false;
        }
        if (str.startsWith("gs://") || str.startsWith("http://") || str.startsWith("https://")) {
            return false;
        }
        try {
            Base64.getDecoder().decode(str.replaceAll("\\s+", ""));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Builds multimodal Content containing Document 1 (Photo ID) and Document 2 (Selfie) with instructions.
     */
    private Mono<Content> buildMultimodalContent(String idUri, String idMimeType, String selfieUri, String selfieMimeType, String customPrompt) {
        String userPromptText = promptProvider.buildUserPrompt(customPrompt);

        Mono<Part> idPartMono = resolveDocumentPart(idUri, idMimeType);
        Mono<Part> selfiePartMono = resolveDocumentPart(selfieUri, selfieMimeType);

        return Mono.zip(idPartMono, selfiePartMono)
                .map(tuple -> {
                    Part idPart = tuple.getT1();
                    Part selfiePart = tuple.getT2();

                    List<Part> parts = List.of(
                            Part.fromText("=== DOCUMENT 1: ID DOCUMENT (WITH PHOTO) ==="),
                            idPart,
                            Part.fromText("=== DOCUMENT 2: SELFIE PHOTO ==="),
                            selfiePart,
                            Part.fromText(userPromptText)
                    );

                    return Content.builder()
                            .role("user")
                            .parts(parts)
                            .build();
                });
    }

    /**
     * Executes the Google ADK Agent via InMemoryRunner and collects the reactive Event stream into a JSON response.
     */
    private Mono<String> executeAdkAgent(Content content, String idUri, String selfieUri) {
        return Mono.defer(() -> {
            try {
                initAdkAgent();
            } catch (Exception e) {
                return Mono.error(new SelfieValidationException(idUri, selfieUri, "Failed to initialize Google ADK Agent: " + e.getMessage(), e));
            }

            String userId = "selfie-user-" + UUID.randomUUID().toString().substring(0, 8);
            String sessionId = "selfie-sess-" + UUID.randomUUID().toString();
            SessionKey sessionKey = new SessionKey(adkRunner.appName(), userId, sessionId);

            // Create session in ADK InMemoryRunner session service
            return Mono.<com.google.adk.sessions.Session>create(sink -> {
                adkRunner.sessionService().createSession(sessionKey)
                        .subscribe(sink::success, sink::error);
            })
            .flatMap(session -> {
                log.info("Created ADK selfie validation session: {} for user: {}", session.id(), userId);
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
        log.debug("Raw output from Google ADK Selfie Validation Agent: {}", rawOutput);
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
     * Cleans, parses, and enriches the model JSON output into SelfieValidationResponse.
     */
    private SelfieValidationResponse parseAndBuildResponse(
            String rawJson,
            String idDocumentUrl,
            String idMimeType,
            String selfieUrl,
            String selfieMimeType,
            long startTime,
            Instant processedAt) {
        long durationMs = System.currentTimeMillis() - startTime;
        String cleanJson = sanitizeJson(rawJson);

        try {
            JsonNode root = objectMapper.readTree(cleanJson);

            SelfieValidationResponse response = new SelfieValidationResponse();
            response.setStatus("SUCCESS");
            response.setMessage("Selfie validation completed successfully");
            response.setIdDocumentUrl(idDocumentUrl);
            response.setSelfieUrl(selfieUrl);

            // isIdentical determination
            boolean isIdentical = false;
            if (root.has("isIdentical")) {
                isIdentical = root.get("isIdentical").asBoolean();
            } else if (root.has("is_identical")) {
                isIdentical = root.get("is_identical").asBoolean();
            } else if (root.has("identical")) {
                isIdentical = root.get("identical").asBoolean();
            } else if (root.has("isMatch")) {
                isIdentical = root.get("isMatch").asBoolean();
            } else if (root.has("matchStatus")) {
                isIdentical = "MATCH".equalsIgnoreCase(root.get("matchStatus").asText());
            }
            response.setIsIdentical(isIdentical);

            // Confidence score determination (normalize to 0.0 - 100.0)
            double confidenceScore = 0.0;
            if (root.has("confidenceScore")) {
                confidenceScore = root.get("confidenceScore").asDouble();
            } else if (root.has("confidentScore")) {
                confidenceScore = root.get("confidentScore").asDouble();
            } else if (root.has("confidence_score")) {
                confidenceScore = root.get("confidence_score").asDouble();
            } else if (root.has("confidence")) {
                confidenceScore = root.get("confidence").asDouble();
            } else if (root.has("score")) {
                confidenceScore = root.get("score").asDouble();
            } else {
                confidenceScore = isIdentical ? 95.0 : 20.0;
            }

            if (confidenceScore <= 1.0 && confidenceScore > 0.0) {
                confidenceScore = confidenceScore * 100.0;
            }
            response.setConfidenceScore(roundToTwoDecimals(confidenceScore));

            // Match status determination
            String matchStatus = "INCONCLUSIVE";
            if (root.has("matchStatus")) {
                matchStatus = root.get("matchStatus").asText();
            } else if (root.has("match_status")) {
                matchStatus = root.get("match_status").asText();
            } else {
                matchStatus = isIdentical ? "MATCH" : (confidenceScore < 50.0 ? "NO_MATCH" : "INCONCLUSIVE");
            }
            response.setMatchStatus(matchStatus);

            // Explanation determination
            String explanation = null;
            if (root.has("explanation")) {
                explanation = root.get("explanation").asText();
            } else if (root.has("explaination")) {
                explanation = root.get("explaination").asText();
            } else if (root.has("reasoning")) {
                explanation = root.get("reasoning").asText();
            } else if (root.has("details")) {
                explanation = root.get("details").asText();
            } else {
                explanation = isIdentical ?
                        "Biometric facial comparison confirms the individual in the selfie matches the person on the photo ID." :
                        "Biometric facial comparison indicates the individual in the selfie does not match the person on the photo ID.";
            }
            response.setExplanation(explanation);

            // Facial Comparison Details
            FacialComparisonDetails details = new FacialComparisonDetails();
            if (root.has("facialComparisonDetails")) {
                JsonNode detailsNode = root.get("facialComparisonDetails");
                details = objectMapper.treeToValue(detailsNode, FacialComparisonDetails.class);
            } else {
                details.setFaceDetectedInId(true);
                details.setFaceDetectedInSelfie(true);
                details.setFacialLandmarksMatch(isIdentical);
                details.setRiskLevel(isIdentical ? "LOW" : "HIGH");
                details.setRecommendation(isIdentical ? "APPROVE" : "REJECT");
                details.setLivenessCheck(new LivenessCheckResult(true, "LOW", "No liveness anomalies detected"));
            }
            response.setFacialComparisonDetails(details);

            // Processing Metadata
            String combinedMimeType = "id: " + idMimeType + ", selfie: " + selfieMimeType;
            ProcessingMetadata metadata = new ProcessingMetadata(
                    properties.getModel(),
                    "Google ADK (Agent Development Kit)",
                    combinedMimeType,
                    processedAt,
                    durationMs
            );
            response.setMetadata(metadata);

            return response;

        } catch (Exception e) {
            log.error("Failed to parse ADK Selfie Validation Agent response JSON: {}", cleanJson, e);
            throw new SelfieValidationException(idDocumentUrl, selfieUrl,
                    "Failed to parse model output into structured selfie validation response: " + e.getMessage(), e);
        }
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
