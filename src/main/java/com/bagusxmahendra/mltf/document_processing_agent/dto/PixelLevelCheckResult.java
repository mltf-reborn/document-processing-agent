package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Detailed result of pixel-level tampering and integrity verification.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PixelLevelCheckResult {

    @JsonProperty("isTampered")
    @JsonAlias({"tampered", "is_tampered"})
    private boolean isTampered;

    @JsonProperty("tamperingRiskLevel")
    @JsonAlias({"tampering_risk_level", "riskLevel", "risk_level"})
    private String tamperingRiskLevel; // "NONE", "LOW", "MEDIUM", "HIGH", "CRITICAL"

    @JsonProperty("tamperingConfidence")
    @JsonAlias({"tampering_confidence"})
    private double tamperingConfidence; // 0.0 - 100.0%

    @JsonProperty("findings")
    private String findings;

    @JsonProperty("anomalies")
    private List<PixelAnomaly> anomalies = new ArrayList<>();

    public PixelLevelCheckResult() {
    }

    public PixelLevelCheckResult(boolean isTampered, String tamperingRiskLevel, double tamperingConfidence, String findings, List<PixelAnomaly> anomalies) {
        this.isTampered = isTampered;
        this.tamperingRiskLevel = tamperingRiskLevel;
        this.tamperingConfidence = tamperingConfidence;
        this.findings = findings;
        this.anomalies = anomalies != null ? anomalies : new ArrayList<>();
    }

    @JsonProperty("isTampered")
    public boolean isTampered() {
        return isTampered;
    }

    public void setTampered(boolean tampered) {
        isTampered = tampered;
    }

    public String getTamperingRiskLevel() {
        return tamperingRiskLevel;
    }

    public void setTamperingRiskLevel(String tamperingRiskLevel) {
        this.tamperingRiskLevel = tamperingRiskLevel;
    }

    public double getTamperingConfidence() {
        return tamperingConfidence;
    }

    public void setTamperingConfidence(double tamperingConfidence) {
        this.tamperingConfidence = tamperingConfidence;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }

    public List<PixelAnomaly> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<PixelAnomaly> anomalies) {
        this.anomalies = anomalies;
    }
}
