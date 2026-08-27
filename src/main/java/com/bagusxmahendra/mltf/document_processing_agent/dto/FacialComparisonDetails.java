package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacialComparisonDetails {

    @JsonProperty("faceDetectedInId")
    @JsonAlias({"face_detected_in_id", "idFaceDetected"})
    private boolean faceDetectedInId = true;

    @JsonProperty("faceDetectedInSelfie")
    @JsonAlias({"face_detected_in_selfie", "selfieFaceDetected"})
    private boolean faceDetectedInSelfie = true;

    @JsonProperty("facialLandmarksMatch")
    @JsonAlias({"facial_landmarks_match", "landmarksMatch", "landmarks_match"})
    private boolean facialLandmarksMatch = true;

    @JsonProperty("matchingFeatures")
    @JsonAlias({"matching_features", "matchedFeatures", "similarities"})
    private List<String> matchingFeatures = new ArrayList<>();

    @JsonProperty("discrepantFeatures")
    @JsonAlias({"discrepant_features", "discrepancies", "differences"})
    private List<String> discrepantFeatures = new ArrayList<>();

    @JsonProperty("livenessCheck")
    @JsonAlias({"liveness_check", "liveness"})
    private LivenessCheckResult livenessCheck;

    @JsonProperty("riskLevel")
    @JsonAlias({"risk_level", "overallRiskLevel"})
    private String riskLevel = "LOW"; // NONE, LOW, MEDIUM, HIGH, CRITICAL

    @JsonProperty("recommendation")
    @JsonAlias({"decision", "action"})
    private String recommendation = "APPROVE"; // APPROVE, REJECT, MANUAL_REVIEW

    public FacialComparisonDetails() {
    }

    public FacialComparisonDetails(boolean faceDetectedInId, boolean faceDetectedInSelfie, boolean facialLandmarksMatch,
                                   List<String> matchingFeatures, List<String> discrepantFeatures,
                                   LivenessCheckResult livenessCheck, String riskLevel, String recommendation) {
        this.faceDetectedInId = faceDetectedInId;
        this.faceDetectedInSelfie = faceDetectedInSelfie;
        this.facialLandmarksMatch = facialLandmarksMatch;
        this.matchingFeatures = matchingFeatures != null ? matchingFeatures : new ArrayList<>();
        this.discrepantFeatures = discrepantFeatures != null ? discrepantFeatures : new ArrayList<>();
        this.livenessCheck = livenessCheck;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
    }

    public boolean isFaceDetectedInId() {
        return faceDetectedInId;
    }

    public void setFaceDetectedInId(boolean faceDetectedInId) {
        this.faceDetectedInId = faceDetectedInId;
    }

    public boolean isFaceDetectedInSelfie() {
        return faceDetectedInSelfie;
    }

    public void setFaceDetectedInSelfie(boolean faceDetectedInSelfie) {
        this.faceDetectedInSelfie = faceDetectedInSelfie;
    }

    public boolean isFacialLandmarksMatch() {
        return facialLandmarksMatch;
    }

    public void setFacialLandmarksMatch(boolean facialLandmarksMatch) {
        this.facialLandmarksMatch = facialLandmarksMatch;
    }

    public List<String> getMatchingFeatures() {
        return matchingFeatures;
    }

    public void setMatchingFeatures(List<String> matchingFeatures) {
        this.matchingFeatures = matchingFeatures != null ? matchingFeatures : new ArrayList<>();
    }

    public List<String> getDiscrepantFeatures() {
        return discrepantFeatures;
    }

    public void setDiscrepantFeatures(List<String> discrepantFeatures) {
        this.discrepantFeatures = discrepantFeatures != null ? discrepantFeatures : new ArrayList<>();
    }

    public LivenessCheckResult getLivenessCheck() {
        return livenessCheck;
    }

    public void setLivenessCheck(LivenessCheckResult livenessCheck) {
        this.livenessCheck = livenessCheck;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
