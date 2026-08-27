package com.bagusxmahendra.mltf.document_processing_agent.controller;

import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingResponse;
import com.bagusxmahendra.mltf.document_processing_agent.service.DocumentProcessingAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Reactive REST Controller exposing the document processing API.
 */
@RestController
@RequestMapping("/api/v1/doc")
public class DocumentProcessingController {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingController.class);

    private final DocumentProcessingAgentService documentProcessingAgentService;

    public DocumentProcessingController(DocumentProcessingAgentService documentProcessingAgentService) {
        this.documentProcessingAgentService = documentProcessingAgentService;
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

        log.info("Received POST /api/v1/doc/processing for GCS URL: {}", effectiveRequest.getGcsUrl());

        return documentProcessingAgentService.processDocument(effectiveRequest)
                .map(ResponseEntity::ok);
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
            return Mono.error(new IllegalArgumentException("Query parameter 'gcsUrl' or 'gcs_url' is required"));
        }

        DocumentProcessingRequest request = new DocumentProcessingRequest(
                url.trim(),
                mimeTypeParam,
                documentTypeParam,
                customPromptParam
        );

        log.info("Received GET /api/v1/doc/processing for GCS URL: {}", request.getGcsUrl());

        return documentProcessingAgentService.processDocument(request)
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
}
