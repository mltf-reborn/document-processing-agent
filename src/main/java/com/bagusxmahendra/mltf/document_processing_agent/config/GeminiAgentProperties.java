package com.bagusxmahendra.mltf.document_processing_agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.gemini")
public class GeminiAgentProperties {

    /**
     * Gemini API Key for Google AI Studio / Gemini Developer API.
     */
    private String apiKey;

    /**
     * Gemini model name (default: gemini-3.5-flash-lite).
     */
    private String model = "gemini-3.5-flash-lite";

    /**
     * GCP Project ID if using Vertex AI.
     */
    private String projectId;

    /**
     * GCP Location / Region if using Vertex AI.
     */
    private String location = "us-central1";

    /**
     * Flag whether to use Vertex AI instead of Gemini Developer API.
     */
    private boolean useVertexAi = false;

    /**
     * Generation temperature (default: 0.1 for deterministic document analysis).
     */
    private float temperature = 0.1f;

    /**
     * Request timeout in seconds.
     */
    private int timeoutSeconds = 90;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isUseVertexAi() {
        return useVertexAi;
    }

    public void setUseVertexAi(boolean useVertexAi) {
        this.useVertexAi = useVertexAi;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
