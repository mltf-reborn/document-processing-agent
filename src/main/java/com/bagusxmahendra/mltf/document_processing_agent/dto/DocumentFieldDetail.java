package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Detailed breakdown of an individual extracted field.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentFieldDetail {

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private Object value;

    @JsonProperty("confidence")
    private double confidence; // 0.0 - 1.0 or 0.0 - 100.0%

    @JsonProperty("isSuspicious")
    @JsonAlias({"suspicious", "is_suspicious"})
    private boolean isSuspicious;

    @JsonProperty("notes")
    private String notes;

    public DocumentFieldDetail() {
    }

    public DocumentFieldDetail(String key, Object value, double confidence, boolean isSuspicious, String notes) {
        this.key = key;
        this.value = value;
        this.confidence = confidence;
        this.isSuspicious = isSuspicious;
        this.notes = notes;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @JsonProperty("isSuspicious")
    public boolean isSuspicious() {
        return isSuspicious;
    }

    public void setSuspicious(boolean suspicious) {
        isSuspicious = suspicious;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
