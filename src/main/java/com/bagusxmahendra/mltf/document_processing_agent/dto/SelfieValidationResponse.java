package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload for selfie validation endpoint.
 * Exclusively provides facial verification, confidence score, and explanation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SelfieValidationResponse {

    private String status; // "SUCCESS", "FAILED"
    private String message;

    @JsonProperty("isIdentical")
    @JsonAlias({"is_identical", "identical", "isMatch", "is_match", "matched"})
    private Boolean isIdentical;

    @JsonProperty("confidenceScore")
    @JsonAlias({"confidentScore", "confident_score", "confidence_score", "confidence", "score", "matchScore"})
    private Double confidenceScore;

    @JsonProperty("matchStatus")
    @JsonAlias({"match_status", "statusMatch", "verdict"})
    private String matchStatus; // "MATCH", "NO_MATCH", "INCONCLUSIVE"

    @JsonProperty("explanation")
    @JsonAlias({"explaination", "description", "reasoning", "details"})
    private String explanation;

    private String idDocumentUrl;
    private String selfieUrl;

    @JsonProperty("facialComparisonDetails")
    @JsonAlias({"facial_comparison_details", "comparisonDetails", "facialComparison"})
    private FacialComparisonDetails facialComparisonDetails;

    private ProcessingMetadata metadata;

    public SelfieValidationResponse() {
    }

    public static SelfieValidationResponse error(String idDocumentUrl, String selfieUrl, String message) {
        SelfieValidationResponse response = new SelfieValidationResponse();
        response.setStatus("FAILED");
        response.setMessage(message);
        response.setIdDocumentUrl(idDocumentUrl);
        response.setSelfieUrl(selfieUrl);
        response.setIsIdentical(false);
        response.setConfidenceScore(0.0);
        response.setMatchStatus("NO_MATCH");
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

    public Boolean getIsIdentical() {
        return isIdentical;
    }

    public void setIsIdentical(Boolean identical) {
        isIdentical = identical;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    // Convenience getter for 'confidentScore'
    public Double getConfidentScore() {
        return confidenceScore;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getIdDocumentUrl() {
        return idDocumentUrl;
    }

    public void setIdDocumentUrl(String idDocumentUrl) {
        this.idDocumentUrl = idDocumentUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public void setSelfieUrl(String selfieUrl) {
        this.selfieUrl = selfieUrl;
    }

    public FacialComparisonDetails getFacialComparisonDetails() {
        return facialComparisonDetails;
    }

    public void setFacialComparisonDetails(FacialComparisonDetails facialComparisonDetails) {
        this.facialComparisonDetails = facialComparisonDetails;
    }

    public ProcessingMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ProcessingMetadata metadata) {
        this.metadata = metadata;
    }
}
