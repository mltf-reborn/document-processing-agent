package com.bagusxmahendra.mltf.document_processing_agent.exception;

public class DocumentProcessingException extends RuntimeException {

    private final String gcsUrl;

    public DocumentProcessingException(String message) {
        super(message);
        this.gcsUrl = null;
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.gcsUrl = null;
    }

    public DocumentProcessingException(String gcsUrl, String message) {
        super(message);
        this.gcsUrl = gcsUrl;
    }

    public DocumentProcessingException(String gcsUrl, String message, Throwable cause) {
        super(message, cause);
        this.gcsUrl = gcsUrl;
    }

    public String getGcsUrl() {
        return gcsUrl;
    }
}
