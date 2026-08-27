package com.bagusxmahendra.mltf.document_processing_agent.exception;

/**
 * Exception thrown when selfie validation processing fails.
 */
public class SelfieValidationException extends RuntimeException {

    private final String idDocumentUrl;
    private final String selfieUrl;

    public SelfieValidationException(String idDocumentUrl, String selfieUrl, String message) {
        super(message);
        this.idDocumentUrl = idDocumentUrl;
        this.selfieUrl = selfieUrl;
    }

    public SelfieValidationException(String idDocumentUrl, String selfieUrl, String message, Throwable cause) {
        super(message, cause);
        this.idDocumentUrl = idDocumentUrl;
        this.selfieUrl = selfieUrl;
    }

    public String getIdDocumentUrl() {
        return idDocumentUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }
}
