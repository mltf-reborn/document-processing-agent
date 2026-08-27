package com.bagusxmahendra.mltf.document_processing_agent.controller;

import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationRequest;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationResponse;
import com.bagusxmahendra.mltf.document_processing_agent.service.SelfieValidationAgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Dedicated REST Controller for selfie verification and facial comparison API.
 * Exposes /api/v1/selfie, /api/v1/selfie/validation, and /api/v1/selfie/validate endpoints.
 */
@RestController
@RequestMapping("/api/v1/selfie")
public class SelfieValidationController {

    private static final Logger log = LoggerFactory.getLogger(SelfieValidationController.class);

    private final SelfieValidationAgentService selfieValidationAgentService;

    public SelfieValidationController(SelfieValidationAgentService selfieValidationAgentService) {
        this.selfieValidationAgentService = selfieValidationAgentService;
    }

    /**
     * Primary endpoint: POST /api/v1/selfie/validation (or POST /api/v1/selfie)
     */
    @PostMapping(
            value = {"", "/validation", "/validate"},
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
        SelfieValidationRequest effectiveRequest = resolveRequest(
                requestBody,
                firstNonNull(idDocUrlParam, idDocUrlSnakeParam, idGcsUrlParam, idGcsUrlSnakeParam),
                firstNonNull(selfieUrlParam, selfieUrlSnakeParam, selfieGcsUrlParam, selfieGcsUrlSnakeParam),
                firstNonNull(idMimeTypeParam, idMimeTypeSnakeParam),
                firstNonNull(selfieMimeTypeParam, selfieMimeTypeSnakeParam),
                customPromptParam
        );

        log.info("Received POST /api/v1/selfie/validation for ID: [{}], Selfie: [{}]",
                effectiveRequest.getIdDocumentUrl(), effectiveRequest.getSelfieUrl());

        return selfieValidationAgentService.validateSelfie(effectiveRequest)
                .map(ResponseEntity::ok);
    }

    /**
     * Convenience GET endpoint: GET /api/v1/selfie/validation?idDocumentUrl=...&selfieUrl=...
     */
    @GetMapping(
            value = {"", "/validation", "/validate"},
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

        log.info("Received GET /api/v1/selfie/validation for ID: [{}], Selfie: [{}]", idUrl, selfieUrl);

        return selfieValidationAgentService.validateSelfie(request)
                .map(ResponseEntity::ok);
    }

    private SelfieValidationRequest resolveRequest(
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
