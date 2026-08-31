package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a pixel-level anomaly detected in a document region or text field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PixelAnomaly {

    @JsonProperty("targetField")
    @JsonAlias({"target_field", "field", "fieldName", "field_name"})
    private String targetField;

    @JsonProperty("anomalyType")
    @JsonAlias({"anomaly_type", "type", "category"})
    private String anomalyType;

    @JsonProperty("severity")
    @JsonAlias({"level", "severityLevel", "severity_level"})
    private String severity;

    @JsonProperty("description")
    @JsonAlias({"desc", "message", "details", "finding", "findings"})
    private String description;

    @JsonProperty("location")
    @JsonAlias({"bbox", "region", "coordinates", "bounds"})
    private String location;

    public PixelAnomaly() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PixelAnomaly(String description) {
        this.description = description;
        this.anomalyType = "TAMPERING";
        this.severity = "MEDIUM";
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

