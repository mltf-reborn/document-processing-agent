package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LivenessCheckResult {

    @JsonProperty("isLive")
    @JsonAlias({"is_live", "live", "isAuthentic", "is_authentic"})
    private boolean isLive = true;

    @JsonProperty("spoofRiskLevel")
    @JsonAlias({"spoof_risk_level", "riskLevel", "risk_level"})
    private String spoofRiskLevel = "NONE"; // NONE, LOW, MEDIUM, HIGH, CRITICAL

    @JsonProperty("findings")
    @JsonAlias({"details", "notes", "description"})
    private String findings;

    public LivenessCheckResult() {
    }

    public LivenessCheckResult(boolean isLive, String spoofRiskLevel, String findings) {
        this.isLive = isLive;
        this.spoofRiskLevel = spoofRiskLevel;
        this.findings = findings;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getSpoofRiskLevel() {
        return spoofRiskLevel;
    }

    public void setSpoofRiskLevel(String spoofRiskLevel) {
        this.spoofRiskLevel = spoofRiskLevel;
    }

    public String getFindings() {
        return findings;
    }

    public void setFindings(String findings) {
        this.findings = findings;
    }
}
