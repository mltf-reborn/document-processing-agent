package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Combined document score, originality score, and confidence score.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentScores {

    private double documentScore;    // 0.0 - 100.0% (Overall combined score)
    private double originalityScore; // 0.0 - 100.0% (Pixel authenticity & integrity score)
    private double confidenceScore;  // 0.0 - 100.0% (Legibility & extraction confidence score)
    private String scoringBreakdown; // Detailed justification of the scores

    public DocumentScores() {
    }

    public DocumentScores(double documentScore, double originalityScore, double confidenceScore, String scoringBreakdown) {
        this.documentScore = documentScore;
        this.originalityScore = originalityScore;
        this.confidenceScore = confidenceScore;
        this.scoringBreakdown = scoringBreakdown;
    }

    public double getDocumentScore() {
        return documentScore;
    }

    public void setDocumentScore(double documentScore) {
        this.documentScore = documentScore;
    }

    public double getOriginalityScore() {
        return originalityScore;
    }

    public void setOriginalityScore(double originalityScore) {
        this.originalityScore = originalityScore;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getScoringBreakdown() {
        return scoringBreakdown;
    }

    public void setScoringBreakdown(String scoringBreakdown) {
        this.scoringBreakdown = scoringBreakdown;
    }
}
