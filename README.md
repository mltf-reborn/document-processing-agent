# Forensic Document Processing Agent

An enterprise-grade, reactive forensic document intelligence microservice built with **Java 25**, **Spring Boot (WebFlux)**, and **Google's Agent Development Kit (ADK)** (`com.google.adk`), powered by **Google Gemini** (`gemini-3.5-flash-lite`).

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

4. **Google ADK `FunctionTool` Capabilities**:
   - Equips the `LlmAgent` with programmatic verification functions:
     - `validateMathCalculations`: Mathematical cross-checks for line items, subtotals, tax rates, discounts, and grand totals to identify forged numbers with 100% arithmetic precision.
     - `verifyChecksum`: Validates checksums (e.g., Luhn Mod 10 for card/account numbers, ID numbers).
     - `validateDateSequence`: Validates chronological timeline consistency (issue date $\le$ due date, validity period).

5. **Multi-modal GCS Ingestion**:
   - Ingests documents directly from Google Cloud Storage (`gs://...` and HTTPS Cloud Storage URLs).
   - Supports inline binary streaming and direct `FileData` fallback references.

---

## 🏗️ Architecture

```
                                  ┌───────────────────────────────┐
                                  │   DocumentProcessingController│
                                  │   (POST/GET /api/v1/doc/...)  │
                                  └───────────────┬───────────────┘
                                                  │ (Reactive Mono/Flux)
                                                  ▼
                                  ┌───────────────────────────────┐
                                  │ DocumentProcessingAgentService│
                                  └───────┬───────────────┬───────┘
                                          │               │
                     ┌────────────────────┴──┐         ┌──┴────────────────────┐
                     │ Google ADK (LlmAgent) │         │   GcsStorageService   │
                     │  - InMemoryRunner     │         │ (Download / Normalize)│
                     │  - Gemini Model       │         └───────────────────────┘
                     └──────────┬────────────┘
                                │
                    ┌───────────┴───────────┐
                    │ DocumentForensicTools │
                    │  - Math Verification  │
                    │  - Checksum (Luhn)    │
                    │  - Date Chronology    │
                    └───────────────────────┘
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

---

## 🚀 API Endpoints

### 1. POST `/api/v1/doc/processing`

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

### 2. GET `/api/v1/doc/processing`

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
