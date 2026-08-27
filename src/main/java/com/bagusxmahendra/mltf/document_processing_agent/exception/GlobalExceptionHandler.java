package com.bagusxmahendra.mltf.document_processing_agent.exception;

import com.bagusxmahendra.mltf.document_processing_agent.dto.DocumentProcessingResponse;
import com.bagusxmahendra.mltf.document_processing_agent.dto.SelfieValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentProcessingException.class)
    public Mono<ResponseEntity<DocumentProcessingResponse>> handleDocumentProcessingException(DocumentProcessingException ex) {
        log.error("Document processing error for URL [{}]: {}", ex.getGcsUrl(), ex.getMessage(), ex);
        DocumentProcessingResponse response = DocumentProcessingResponse.error(
                ex.getGcsUrl(),
                ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response));
    }

    @ExceptionHandler(SelfieValidationException.class)
    public Mono<ResponseEntity<SelfieValidationResponse>> handleSelfieValidationException(SelfieValidationException ex) {
        log.error("Selfie validation error for ID [{}] and Selfie [{}]: {}", ex.getIdDocumentUrl(), ex.getSelfieUrl(), ex.getMessage(), ex);
        SelfieValidationResponse response = SelfieValidationResponse.error(
                ex.getIdDocumentUrl(),
                ex.getSelfieUrl(),
                ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(response));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<DocumentProcessingResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Invalid input argument: {}", ex.getMessage());
        DocumentProcessingResponse response = DocumentProcessingResponse.error(
                null,
                ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<DocumentProcessingResponse>> handleServerWebInputException(ServerWebInputException ex) {
        String reason = ex.getMessage() != null ? ex.getMessage() : "Invalid input payload";
        log.warn("Web input error: {}", reason);
        DocumentProcessingResponse response = DocumentProcessingResponse.error(
                null,
                "Bad request: " + reason
        );
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<DocumentProcessingResponse>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        DocumentProcessingResponse response = DocumentProcessingResponse.error(
                null,
                "An internal server error occurred: " + ex.getMessage()
        );
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}
