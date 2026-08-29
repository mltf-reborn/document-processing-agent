package com.bagusxmahendra.mltf.document_processing_agent.controller;

import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingResponse;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationResponse;
import com.bagusxmahendra.mltf.document_processing_agent.service.DocumentProcessingAgentService;
import com.bagusxmahendra.mltf.document_processing_agent.service.SelfieValidationAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Reactive REST Controller exposing document processing and selfie validation APIs.
 */
@RestController
@RequestMapping("/api/v1/doc")
public class DocumentProcessingController {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingController.class);

    private final DocumentProcessingAgentService documentProcessingAgentService;
    private final SelfieValidationAgentService selfieValidationAgentService;
    private final ObjectMapper objectMapper;

    public DocumentProcessingController(
            DocumentProcessingAgentService documentProcessingAgentService,
            SelfieValidationAgentService selfieValidationAgentService) {
        this.documentProcessingAgentService = documentProcessingAgentService;
        this.selfieValidationAgentService = selfieValidationAgentService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Primary endpoint: POST /api/v1/doc/processing with JSON payload.
     * Also supports query parameters when body is omitted or partially specified.
     */
    @PostMapping(
            value = "/processing",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<DocumentProcessingResponse>> processDocumentPost(
            @RequestBody(required = false) DocumentProcessingRequest requestBody,
            @RequestParam(name = "gcsUrl", required = false) String gcsUrlParam,
            @RequestParam(name = "gcs_url", required = false) String gcsUrlSnakeParam,
            @RequestParam(name = "mimeType", required = false) String mimeTypeParam,
            @RequestParam(name = "documentType", required = false) String documentTypeParam,
            @RequestParam(name = "customPrompt", required = false) String customPromptParam
    ) {
        DocumentProcessingRequest effectiveRequest = resolveRequest(
                requestBody,
                gcsUrlParam != null ? gcsUrlParam : gcsUrlSnakeParam,
                mimeTypeParam,
                documentTypeParam,
                customPromptParam
        );

        log.info(">>> [REQUEST] POST /api/v1/doc/processing\nPayload: {}", toJson(effectiveRequest));

        return documentProcessingAgentService.processDocument(effectiveRequest)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("<<< [RESPONSE] POST /api/v1/doc/processing (HTTP {})\nBody: {}",
                        response.getStatusCode(), toJson(response.getBody())))
                .doOnError(err -> log.error("<<< [RESPONSE ERROR] POST /api/v1/doc/processing failed: {}", err.getMessage(), err));
    }

    /**
     * Convenience GET endpoint: GET /api/v1/doc/processing?gcsUrl=gs://...
     */
    @GetMapping(
            value = "/processing",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<DocumentProcessingResponse>> processDocumentGet(
            @RequestParam(name = "gcsUrl", required = false) String gcsUrlParam,
            @RequestParam(name = "gcs_url", required = false) String gcsUrlSnakeParam,
            @RequestParam(name = "mimeType", required = false) String mimeTypeParam,
            @RequestParam(name = "documentType", required = false) String documentTypeParam,
            @RequestParam(name = "customPrompt", required = false) String customPromptParam
    ) {
        String url = gcsUrlParam != null ? gcsUrlParam : gcsUrlSnakeParam;
        if (url == null || url.trim().isEmpty()) {
            log.warn(">>> [REQUEST] GET /api/v1/doc/processing - Missing required 'gcsUrl' or 'gcs_url' query parameter");
            return Mono.error(new IllegalArgumentException("Query parameter 'gcsUrl' or 'gcs_url' is required"));
        }

        DocumentProcessingRequest request = new DocumentProcessingRequest(
                url.trim(),
                mimeTypeParam,
                documentTypeParam,
                customPromptParam
        );

        log.info(">>> [REQUEST] GET /api/v1/doc/processing\nParams: {}", toJson(request));

        return documentProcessingAgentService.processDocument(request)
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("<<< [RESPONSE] GET /api/v1/doc/processing (HTTP {})\nBody: {}",
                        response.getStatusCode(), toJson(response.getBody())))
                .doOnError(err -> log.error("<<< [RESPONSE ERROR] GET /api/v1/doc/processing failed: {}", err.getMessage(), err));
    }

    /**
     * Selfie Validation endpoint: POST /api/v1/doc/selfie-validation
     */
    @PostMapping(
            value = {"/selfie-validation", "/validate-selfie"},
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<SelfieValidationResponse>> validateSelfiePost(
            @RequestBody(required = false) SelfieValidationRequest requestBody,
            @RequestParam(name = "idDocumentUrl", required = false) String idDocUrlParam,
            @RequestParam(name = "id_document_url", required = false) String idDocUrlSnakeParam,
            @RequestParam(name = "idGcsUrl", required = false) String idGcsUrlParam,
            @RequestParam(name = "id_gcs_url", required = false) String idGcsUrlSnakeParam,
            @RequestParam(name = "selfieUrl", required = false) String selfieUrlParam,
            @RequestParam(name = "selfie_url", required = false) String selfieUrlSnakeParam,
            @RequestParam(name = "selfieGcsUrl", required = false) String selfieGcsUrlParam,
            @RequestParam(name = "selfie_gcs_url", required = false) String selfieGcsUrlSnakeParam,
            @RequestParam(name = "idMimeType", required = false) String idMimeTypeParam,
            @RequestParam(name = "id_mime_type", required = false) String idMimeTypeSnakeParam,
            @RequestParam(name = "selfieMimeType", required = false) String selfieMimeTypeParam,
            @RequestParam(name = "selfie_mime_type", required = false) String selfieMimeTypeSnakeParam,
            @RequestParam(name = "customPrompt", required = false) String customPromptParam
    ) {
        SelfieValidationRequest effectiveRequest = resolveSelfieRequest(
                requestBody,
                firstNonNull(idDocUrlParam, idDocUrlSnakeParam, idGcsUrlParam, idGcsUrlSnakeParam),
                firstNonNull(selfieUrlParam, selfieUrlSnakeParam, selfieGcsUrlParam, selfieGcsUrlSnakeParam),
                firstNonNull(idMimeTypeParam, idMimeTypeSnakeParam),
                firstNonNull(selfieMimeTypeParam, selfieMimeTypeSnakeParam),
                customPromptParam
        );

        log.info("Received POST /api/v1/doc/selfie-validation for ID: [{}], Selfie: [{}]",
                effectiveRequest.getIdDocumentUrl(), effectiveRequest.getSelfieUrl());

        return selfieValidationAgentService.validateSelfie(effectiveRequest)
                .map(ResponseEntity::ok);
    }

    /**
     * Convenience GET endpoint: GET /api/v1/doc/selfie-validation?idDocumentUrl=...&selfieUrl=...
     */
    @GetMapping(
            value = {"/selfie-validation", "/validate-selfie"},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<SelfieValidationResponse>> validateSelfieGet(
            @RequestParam(name = "idDocumentUrl", required = false) String idDocUrlParam,
            @RequestParam(name = "id_document_url", required = false) String idDocUrlSnakeParam,
            @RequestParam(name = "idGcsUrl", required = false) String idGcsUrlParam,
            @RequestParam(name = "id_gcs_url", required = false) String idGcsUrlSnakeParam,
            @RequestParam(name = "selfieUrl", required = false) String selfieUrlParam,
            @RequestParam(name = "selfie_url", required = false) String selfieUrlSnakeParam,
            @RequestParam(name = "selfieGcsUrl", required = false) String selfieGcsUrlParam,
            @RequestParam(name = "selfie_gcs_url", required = false) String selfieGcsUrlSnakeParam,
            @RequestParam(name = "idMimeType", required = false) String idMimeTypeParam,
            @RequestParam(name = "id_mime_type", required = false) String idMimeTypeSnakeParam,
            @RequestParam(name = "selfieMimeType", required = false) String selfieMimeTypeParam,
            @RequestParam(name = "selfie_mime_type", required = false) String selfieMimeTypeSnakeParam,
            @RequestParam(name = "customPrompt", required = false) String customPromptParam
    ) {
        String idUrl = firstNonNull(idDocUrlParam, idDocUrlSnakeParam, idGcsUrlParam, idGcsUrlSnakeParam);
        String selfieUrl = firstNonNull(selfieUrlParam, selfieUrlSnakeParam, selfieGcsUrlParam, selfieGcsUrlSnakeParam);

        if (idUrl == null || idUrl.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Query parameter 'idDocumentUrl' or 'id_document_url' is required"));
        }
        if (selfieUrl == null || selfieUrl.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Query parameter 'selfieUrl' or 'selfie_url' is required"));
        }

        SelfieValidationRequest request = new SelfieValidationRequest(
                idUrl.trim(),
                selfieUrl.trim(),
                firstNonNull(idMimeTypeParam, idMimeTypeSnakeParam),
                firstNonNull(selfieMimeTypeParam, selfieMimeTypeSnakeParam),
                customPromptParam
        );

        log.info("Received GET /api/v1/doc/selfie-validation for ID: [{}], Selfie: [{}]", idUrl, selfieUrl);

        return selfieValidationAgentService.validateSelfie(request)
                .map(ResponseEntity::ok);
    }

    private DocumentProcessingRequest resolveRequest(
            DocumentProcessingRequest body,
            String gcsUrlParam,
            String mimeTypeParam,
            String documentTypeParam,
            String customPromptParam
    ) {
        DocumentProcessingRequest req = body != null ? body : new DocumentProcessingRequest();

        if ((req.getGcsUrl() == null || req.getGcsUrl().isBlank()) && gcsUrlParam != null && !gcsUrlParam.isBlank()) {
            req.setGcsUrl(gcsUrlParam.trim());
        }
        if ((req.getMimeType() == null || req.getMimeType().isBlank()) && mimeTypeParam != null && !mimeTypeParam.isBlank()) {
            req.setMimeType(mimeTypeParam.trim());
        }
        if ((req.getDocumentType() == null || req.getDocumentType().isBlank()) && documentTypeParam != null && !documentTypeParam.isBlank()) {
            req.setDocumentType(documentTypeParam.trim());
        }
        if ((req.getCustomPrompt() == null || req.getCustomPrompt().isBlank()) && customPromptParam != null && !customPromptParam.isBlank()) {
            req.setCustomPrompt(customPromptParam.trim());
        }

        return req;
    }

    private SelfieValidationRequest resolveSelfieRequest(
            SelfieValidationRequest body,
            String idDocUrlParam,
            String selfieUrlParam,
            String idMimeTypeParam,
            String selfieMimeTypeParam,
            String customPromptParam
    ) {
        SelfieValidationRequest req = body != null ? body : new SelfieValidationRequest();

        if ((req.getIdDocumentUrl() == null || req.getIdDocumentUrl().isBlank()) && idDocUrlParam != null && !idDocUrlParam.isBlank()) {
            req.setIdDocumentUrl(idDocUrlParam.trim());
        }
        if ((req.getSelfieUrl() == null || req.getSelfieUrl().isBlank()) && selfieUrlParam != null && !selfieUrlParam.isBlank()) {
            req.setSelfieUrl(selfieUrlParam.trim());
        }
        if ((req.getIdDocumentMimeType() == null || req.getIdDocumentMimeType().isBlank()) && idMimeTypeParam != null && !idMimeTypeParam.isBlank()) {
            req.setIdDocumentMimeType(idMimeTypeParam.trim());
        }
        if ((req.getSelfieMimeType() == null || req.getSelfieMimeType().isBlank()) && selfieMimeTypeParam != null && !selfieMimeTypeParam.isBlank()) {
            req.setSelfieMimeType(selfieMimeTypeParam.trim());
        }
        if ((req.getCustomPrompt() == null || req.getCustomPrompt().isBlank()) && customPromptParam != null && !customPromptParam.isBlank()) {
            req.setCustomPrompt(customPromptParam.trim());
        }

        return req;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... items) {
        if (items == null) return null;
        for (T item : items) {
            if (item != null) {
                if (item instanceof String s && s.isBlank()) {
                    continue;
                }
                return item;
            }
        }
        return null;
    }
}
