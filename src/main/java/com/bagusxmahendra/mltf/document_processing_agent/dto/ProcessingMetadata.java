package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Technical and execution metadata for document processing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingMetadata {

    private String model;
    private String agentFramework;
    private String detectedMimeType;
    private Instant processedAt;
    private long executionDurationMs;

    public ProcessingMetadata() {
    }

    public ProcessingMetadata(String model, String agentFramework, String detectedMimeType, Instant processedAt, long executionDurationMs) {
        this.model = model;
        this.agentFramework = agentFramework;
        this.detectedMimeType = detectedMimeType;
        this.processedAt = processedAt;
        this.executionDurationMs = executionDurationMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAgentFramework() {
        return agentFramework;
    }

    public void setAgentFramework(String agentFramework) {
        this.agentFramework = agentFramework;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public void setDetectedMimeType(String detectedMimeType) {
        this.detectedMimeType = detectedMimeType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public void setExecutionDurationMs(long executionDurationMs) {
        this.executionDurationMs = executionDurationMs;
    }
}
