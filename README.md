# Forensic Document Processing Agent

An enterprise-grade, reactive forensic document intelligence and identity verification microservice built with **Java 25**, **Spring Boot (WebFlux)**, and **Google's Agent Development Kit (ADK)** (`com.google.adk`), powered by **Google Gemini** (`gemini-3.5-flash-lite`).

---

## 🌟 Key Capabilities

1. **Pixel-Level Forensic Integrity & Tampering Analysis**:
   - Deep multimodal inspection detecting digital document manipulation, forgery, and splicing.
   - Identifies font rendering discrepancies, anti-aliasing edge mismatch, font weight/size inconsistencies, and character baseline misalignments.
   - Pinpoints JPEG compression block boundary anomalies around edited numbers, dates, or financial totals.
   - Detects background tint shifts, digital stamp overlays, copy-paste bounding boxes, and erasure halos.
   - Emits structured tamper status (`isTampered`), severity risk level (`NONE`, `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), confidence percentage, and granular anomaly lists.

2. **Adaptive Dynamic Key-Value Extraction**:
   - Automatically adapts to any document type (invoices, national ID cards, receipts, bank statements, certificates, legal contracts, etc.).
   - Produces a dynamic key-value mapping (`extractedFields`) alongside structured field details (`fieldDetails`) with individual confidence scores and suspicion flags.

3. **Multi-Factor Authenticity Scoring**:
   - **Originality Score (0.0% – 100.0%)**: Document pixel authenticity and absence of digital forgery.
   - **Confidence Score (0.0% – 100.0%)**: OCR clarity, text legibility, and lack of visual obstruction.
   - **Document Score (0.0% – 100.0%)**: Unified composite confidence rating.
   - **Scoring Breakdown**: Human-readable forensic summary.

4. **Biometric Selfie & Photo ID Validation**:
   - Multimodal facial comparison checking if an individual's **Selfie** matches the portrait on their **Photo ID document** (Driver's License, Passport, National ID Card, Residence Permit).
   - Detailed biometric landmark analysis (craniofacial structure, jawline, nose bridge, eye shape, interpupillary distance, mouth contour, earlobes, and distinctive marks/scars).
   - Built-in anti-spoofing and liveness evaluation (detecting screen replays, printed photos, 3D masks, deepfakes, or ID cutout overlays).
   - Returns structured `isIdentical` (boolean), `confidenceScore` (0.0% – 100.0%), `matchStatus` (`MATCH`, `NO_MATCH`, `INCONCLUSIVE`), and comprehensive forensic `explanation`.

5. **Google ADK `FunctionTool` Capabilities**:
   - Equips the `LlmAgent` with programmatic verification functions:
     - `validateMathCalculations`: Mathematical cross-checks for line items, subtotals, tax rates, discounts, and grand totals to identify forged numbers with 100% arithmetic precision.
     - `verifyChecksum`: Validates checksums (e.g., Luhn Mod 10 for card/account numbers, ID numbers).
     - `validateDateSequence`: Validates chronological timeline consistency (issue date $\le$ due date, validity period).

6. **Multi-modal GCS & Binary Ingestion**:
   - Ingests documents and selfies directly from Google Cloud Storage (`gs://...` and HTTPS Cloud Storage URLs), Data URIs (`data:image/...;base64,...`), and raw Base64 payloads.
   - Supports resilient inline binary streaming and direct `FileData` fallback references.

---

## 🏗️ Architecture

```
                                      ┌──────────────────────────────────────────────────────────┐
                                      │              DocumentProcessingController /              │
                                      │                SelfieValidationController                │
                                      │  (POST/GET /api/v1/doc/... | /api/v1/selfie/...)         │
                                      └─────────────┬───────────────────────────────┬────────────┘
                                                    │ (Reactive Mono/Flux)          │
                                                    ▼                               ▼
                                  ┌───────────────────────────────┐   ┌───────────────────────────────┐
                                  │ DocumentProcessingAgentService│   │  SelfieValidationAgentService │
                                  └───────┬───────────────┬───────┘   └───────┬───────────────┬───────┘
                                          │               │                   │               │
                     ┌────────────────────┴──┐         ┌──┴───────────────────┴┐              │
                     │ Google ADK (LlmAgent) │         │   GcsStorageService   │              │
                     │  - InMemoryRunner     │         │ (Download / Normalize)│              │
                     │  - Gemini Model       │         └───────────────────────┘              │
                     └──────────┬────────────┘                                                │
                                │                                                             ▼
                    ┌───────────┴───────────┐                                   ┌───────────────────────────┐
                    │ DocumentForensicTools │                                   │   Biometric Verification  │
                    │  - Math Verification  │                                   │  - Facial Landmarks       │
                    │  - Checksum (Luhn)    │                                   │  - Anti-Spoofing/Liveness │
                    │  - Date Chronology    │                                   │  - Confidence Scoring     │
                    └───────────────────────┘                                   └───────────────────────────┘
```

---

## ⚙️ Configuration & Environment Variables

| Variable | Property | Default | Description |
| :--- | :--- | :--- | :--- |
| `GEMINI_API_KEY` / `GOOGLE_API_KEY` | `google.gemini.api-key` | *None* | Google AI Studio / Gemini API Key |
| `GEMINI_MODEL` | `google.gemini.model` | `gemini-3.5-flash-lite` | Gemini model name |
| `GOOGLE_GENAI_USE_VERTEXAI` | `google.gemini.use-vertex-ai` | `false` | Enable Google Cloud Vertex AI backend |
| `GOOGLE_CLOUD_PROJECT` | `google.gemini.project-id` | *None* | GCP Project ID (for Vertex AI) |
| `GOOGLE_CLOUD_LOCATION` | `google.gemini.location` | `us-central1` | GCP Region (for Vertex AI) |
| `google.gemini.temperature` | `google.gemini.temperature` | `0.1` | Temperature for deterministic output |
| `google.gemini.timeout-seconds` | `google.gemini.timeout-seconds` | `90` | Reactive request timeout in seconds |

---

## 🚀 API Endpoints

### 1. POST `/api/v1/doc/selfie-validation` (or `/api/v1/selfie/validation`)

Validates if a Selfie photograph belongs to the identical person shown on the Photo ID document using multimodal LLM facial comparison.

**Request Body (`application/json`):**
```json
{
  "idDocumentUrl": "gs://my-bucket/kyc/driver_license.png",
  "selfieUrl": "gs://my-bucket/kyc/user_selfie.jpg",
  "idDocumentMimeType": "image/png",
  "selfieMimeType": "image/jpeg",
  "customPrompt": "Perform strict biometric facial comparison"
}
```

**Supported Aliases in Request Body:**
- ID Document: `idDocumentUrl`, `id_document_url`, `idGcsUrl`, `id_gcs_url`, `idDocument`, `id_document`, `idCardUrl`, `document1`, `document1Url`
- Selfie: `selfieUrl`, `selfie_url`, `selfieGcsUrl`, `selfie_gcs_url`, `selfie`, `selfieImage`, `document2`, `document2Url`

**Response (`application/json`):**
```json
{
  "status": "SUCCESS",
  "message": "Selfie validation completed successfully",
  "isIdentical": true,
  "confidenceScore": 96.8,
  "matchStatus": "MATCH",
  "explanation": "Biometric facial comparison confirms that the person in the selfie is identical to the individual depicted on the driver's license. The craniofacial bone structure, interpupillary distance ratio, nasal bridge contour, and chin morphology match with high precision. Liveness analysis shows authentic skin texture and natural lighting with no signs of presentation attack.",
  "idDocumentUrl": "gs://my-bucket/kyc/driver_license.png",
  "selfieUrl": "gs://my-bucket/kyc/user_selfie.jpg",
  "facialComparisonDetails": {
    "faceDetectedInId": true,
    "faceDetectedInSelfie": true,
    "facialLandmarksMatch": true,
    "matchingFeatures": [
      "Identical jawline contour and chin shape",
      "Consistent interpupillary distance and ocular slant",
      "Matching nasal bridge width and tip structure",
      "Consistent earlobe morphology"
    ],
    "discrepantFeatures": [],
    "livenessCheck": {
      "isLive": true,
      "spoofRiskLevel": "LOW",
      "findings": "Authentic skin texture, natural illumination and depth, zero screen bezel, pixel grid, or photo cutout anomalies detected."
    },
    "riskLevel": "LOW",
    "recommendation": "APPROVE"
  },
  "metadata": {
    "model": "gemini-3.5-flash-lite",
    "agentFramework": "Google ADK (Agent Development Kit)",
    "mimeType": "id: image/png, selfie: image/jpeg",
    "processedAt": "2026-08-27T05:00:00Z",
    "processingDurationMs": 1380
  }
}
```

### 2. GET `/api/v1/doc/selfie-validation` (or `/api/v1/selfie/validation`)

**Query Parameters:**
- `idDocumentUrl` / `id_document_url` (*required*): GCS URI or Data URI of the Photo ID.
- `selfieUrl` / `selfie_url` (*required*): GCS URI or Data URI of the Selfie.
- `idDocumentMimeType` (*optional*): MIME type of ID.
- `selfieMimeType` (*optional*): MIME type of Selfie.
- `customPrompt` (*optional*): Custom comparison guidelines.

---

### 3. POST `/api/v1/doc/processing`

Forensic document tampering inspection, pixel anomaly detection, dynamic key-value extraction, and multi-factor scoring.

**Request Body (`application/json`):**
```json
{
  "gcs_url": "gs://my-bucket/documents/invoice_2026.pdf",
  "mime_type": "application/pdf",
  "custom_prompt": "Verify tax calculations and payment terms"
}
```

**Response (`application/json`):**
```json
{
  "status": "SUCCESS",
  "message": "Document processed successfully",
  "gcsUrl": "gs://my-bucket/documents/invoice_2026.pdf",
  "detectedDocumentType": "INVOICE",
  "scores": {
    "documentScore": 98.4,
    "originalityScore": 100.0,
    "confidenceScore": 96.0,
    "scoringBreakdown": "Originality: 100% (zero pixel tampering); Confidence: 96% (crisp text); Document Score: 98.4%"
  },
  "pixelLevelCheck": {
    "isTampered": false,
    "tamperingRiskLevel": "NONE",
    "tamperingConfidence": 0.0,
    "findings": "Pixel analysis confirmed consistent font rendering, uniform compression noise, and authentic background textures.",
    "anomalies": []
  },
  "extractedFields": {
    "invoiceNumber": "INV-2026-001",
    "invoiceDate": "2026-01-15",
    "dueDate": "2026-02-15",
    "vendorName": "Acme Corp",
    "subtotal": "$1,000.00",
    "tax": "$100.00",
    "totalAmount": "$1,100.00"
  },
  "fieldDetails": [
    {
      "key": "invoiceNumber",
      "value": "INV-2026-001",
      "confidence": 0.99,
      "isSuspicious": false,
      "notes": "Consistent font baseline and antialiasing"
    }
  ],
  "metadata": {
    "model": "gemini-3.5-flash-lite",
    "agentFramework": "Google ADK (Agent Development Kit)",
    "mimeType": "application/pdf",
    "processedAt": "2026-08-27T02:40:00Z",
    "processingDurationMs": 1420
  }
}
```

### 4. GET `/api/v1/doc/processing`

**Query Parameters:**
- `gcsUrl` / `gcs_url` (*required*): `gs://my-bucket/path/to/document.png`
- `mimeType` (*optional*): `image/png`
- `customPrompt` (*optional*): Additional inspection instructions.

---

## 🛠️ Build & Test

```bash
# Run all unit tests
./gradlew test

# Run application locally
./gradlew bootRun
```
