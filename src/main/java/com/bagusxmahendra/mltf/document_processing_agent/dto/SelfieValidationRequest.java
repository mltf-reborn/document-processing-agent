package com.bagusxmahendra.mltf.document_processing_agent.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for selfie validation against an ID document with photo.
 */
public class SelfieValidationRequest {

    @JsonProperty("idDocumentUrl")
    @JsonAlias({
            "id_document_url",
            "idGcsUrl",
            "id_gcs_url",
            "idDocument",
            "id_document",
            "idCardUrl",
            "id_card_url",
            "document1",
            "document_1",
            "document1Url",
            "document_1_url",
            "gcsUrl",
            "gcs_url",
            "idUrl",
            "id_url"
    })
    private String idDocumentUrl;

    @JsonProperty("selfieUrl")
    @JsonAlias({
            "selfie_url",
            "selfieGcsUrl",
            "selfie_gcs_url",
            "selfie",
            "selfieImage",
            "selfie_image",
            "selfieDocument",
            "selfie_document",
            "document2",
            "document_2",
            "document2Url",
            "document_2_url",
            "faceUrl",
            "face_url"
    })
    private String selfieUrl;

    @JsonProperty("idDocumentMimeType")
    @JsonAlias({
            "id_document_mime_type",
            "idMimeType",
            "id_mime_type",
            "document1MimeType",
            "document_1_mime_type",
            "mimeType",
            "mime_type"
    })
    private String idDocumentMimeType;

    @JsonProperty("selfieMimeType")
    @JsonAlias({
            "selfie_mime_type",
            "document2MimeType",
            "document_2_mime_type"
    })
    private String selfieMimeType;

    @JsonProperty("customPrompt")
    @JsonAlias({
            "custom_prompt",
            "instructions",
            "additional_instructions",
            "prompt"
    })
    private String customPrompt;

    public SelfieValidationRequest() {
    }

    public SelfieValidationRequest(String idDocumentUrl, String selfieUrl) {
        this.idDocumentUrl = idDocumentUrl;
        this.selfieUrl = selfieUrl;
    }

    public SelfieValidationRequest(String idDocumentUrl, String selfieUrl, String idDocumentMimeType, String selfieMimeType, String customPrompt) {
        this.idDocumentUrl = idDocumentUrl;
        this.selfieUrl = selfieUrl;
        this.idDocumentMimeType = idDocumentMimeType;
        this.selfieMimeType = selfieMimeType;
        this.customPrompt = customPrompt;
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

    public String getIdDocumentMimeType() {
        return idDocumentMimeType;
    }

    public void setIdDocumentMimeType(String idDocumentMimeType) {
        this.idDocumentMimeType = idDocumentMimeType;
    }

    public String getSelfieMimeType() {
        return selfieMimeType;
    }

    public void setSelfieMimeType(String selfieMimeType) {
        this.selfieMimeType = selfieMimeType;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
    }

    @Override
    public String toString() {
        return "SelfieValidationRequest{" +
                "idDocumentUrl='" + idDocumentUrl + '\'' +
                ", selfieUrl='" + selfieUrl + '\'' +
                ", idDocumentMimeType='" + idDocumentMimeType + '\'' +
                ", selfieMimeType='" + selfieMimeType + '\'' +
                '}';
    }
}
