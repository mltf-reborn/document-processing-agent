package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Top-level response for document processing endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentProcessingResponse {

    private String status; // "SUCCESS", "FAILED"
    private String message;
    private String gcsUrl;
    private String detectedDocumentType;
    private DocumentScores scores;
    private PixelLevelCheckResult pixelLevelCheck;
    private Map<String, Object> extractedFields;
    private List<DocumentFieldDetail> fieldDetails;
    private ProcessingMetadata metadata;

    public DocumentProcessingResponse() {
    }

    public static DocumentProcessingResponse error(String gcsUrl, String message) {
        DocumentProcessingResponse response = new DocumentProcessingResponse();
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setGcsUrl(gcsUrl);
        return response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getGcsUrl() {
        return gcsUrl;
    }

    public void setGcsUrl(String gcsUrl) {
        this.gcsUrl = gcsUrl;
    }

    public String getDetectedDocumentType() {
        return detectedDocumentType;
    }

    public void setDetectedDocumentType(String detectedDocumentType) {
        this.detectedDocumentType = detectedDocumentType;
    }

    public DocumentScores getScores() {
        return scores;
    }

    public void setScores(DocumentScores scores) {
        this.scores = scores;
    }

    public PixelLevelCheckResult getPixelLevelCheck() {
        return pixelLevelCheck;
    }

    public void setPixelLevelCheck(PixelLevelCheckResult pixelLevelCheck) {
        this.pixelLevelCheck = pixelLevelCheck;
    }

    public Map<String, Object> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(Map<String, Object> extractedFields) {
        this.extractedFields = extractedFields;
    }

    public List<DocumentFieldDetail> getFieldDetails() {
        return fieldDetails;
    }

    public void setFieldDetails(List<DocumentFieldDetail> fieldDetails) {
        this.fieldDetails = fieldDetails;
    }

    public ProcessingMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ProcessingMetadata metadata) {
        this.metadata = metadata;
    }
}
