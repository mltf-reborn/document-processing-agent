package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a pixel-level anomaly detected in a document region or text field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PixelAnomaly {

    private String targetField;
    private String anomalyType;
    private String severity;
    private String description;
    private String location;

    public PixelAnomaly() {
    }

    public PixelAnomaly(String targetField, String anomalyType, String severity, String description, String location) {
        this.targetField = targetField;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.description = description;
        this.location = location;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getAnomalyType() {
        return anomalyType;
    }

    public void setAnomalyType(String anomalyType) {
        this.anomalyType = anomalyType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
