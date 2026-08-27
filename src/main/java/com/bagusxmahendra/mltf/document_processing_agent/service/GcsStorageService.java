package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.exception.DocumentProcessingException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.Locale;

@Service
public class GcsStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcsStorageService.class);

    private final Storage storage;

    public GcsStorageService() {
        Storage storageInstance = null;
        try {
            storageInstance = StorageOptions.getDefaultInstance().getService();
        } catch (Exception e) {
            log.warn("Google Cloud Storage default client could not be initialized (Application Default Credentials may not be configured): {}", e.getMessage());
        }
        this.storage = storageInstance;
    }

    /**
     * Resolves and normalizes a GCS URL to a standard gs:// format if it's an HTTPS storage URL.
     */
    public String normalizeGcsUri(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("GCS URL parameter must not be null or empty");
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("gs://")) {
            return trimmed;
        }

        if (trimmed.startsWith("https://storage.googleapis.com/") || trimmed.startsWith("http://storage.googleapis.com/")) {
            String path = trimmed.replaceFirst("^https?://storage\\.googleapis\\.com/", "");
            return "gs://" + path;
        }

        if (trimmed.startsWith("https://storage.cloud.google.com/") || trimmed.startsWith("http://storage.cloud.google.com/")) {
            String path = trimmed.replaceFirst("^https?://storage\\.cloud\\.google\\.com/", "");
            return "gs://" + path;
        }

        return trimmed;
    }

    /**
     * Extracts the bucket name and object name from a GCS URI or URL.
     */
    public GcsBlobReference parseGcsReference(String url) {
        String normalized = normalizeGcsUri(url);
        if (!normalized.startsWith("gs://")) {
            throw new DocumentProcessingException(url, "Invalid GCS URL scheme. Must start with gs:// or https://storage.googleapis.com/");
        }

        String pathPart = normalized.substring("gs://".length());
        int slashIndex = pathPart.indexOf('/');
        if (slashIndex <= 0 || slashIndex == pathPart.length() - 1) {
            throw new DocumentProcessingException(url, "Invalid GCS path. Expected format: gs://<bucket-name>/<object-path>");
        }

        String bucketName = pathPart.substring(0, slashIndex);
        String objectName = pathPart.substring(slashIndex + 1);

        return new GcsBlobReference(bucketName, objectName, normalized);
    }

    /**
     * Infers the MIME type based on file extension or provided hint.
     */
    public String detectMimeType(String url, String hintMimeType) {
        if (hintMimeType != null && !hintMimeType.trim().isEmpty()) {
            return hintMimeType.trim().toLowerCase(Locale.ROOT);
        }

        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("?")) {
            lower = lower.substring(0, lower.indexOf('?'));
        }

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".tif") || lower.endsWith(".tiff")) {
            return "image/tiff";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lower.endsWith(".heic")) {
            return "image/heic";
        } else if (lower.endsWith(".heif")) {
            return "image/heif";
        }

        // Default to image/jpeg if unknown image or application/pdf
        return "application/pdf";
    }

    /**
     * Asynchronously downloads bytes from Google Cloud Storage.
     */
    public Mono<byte[]> downloadBlobBytes(String gcsUrl) {
        return Mono.fromCallable(() -> {
            if (storage == null) {
                throw new DocumentProcessingException(gcsUrl, "Google Cloud Storage client is not initialized.");
            }

            GcsBlobReference ref = parseGcsReference(gcsUrl);
            log.info("Fetching document from GCS bucket: '{}', object: '{}'", ref.bucketName(), ref.objectName());

            Blob blob = storage.get(BlobId.of(ref.bucketName(), ref.objectName()));
            if (blob == null || !blob.exists()) {
                throw new DocumentProcessingException(gcsUrl, "Document not found in GCS bucket: " + ref.bucketName() + ", object: " + ref.objectName());
            }

            return blob.getContent();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public record GcsBlobReference(String bucketName, String objectName, String fullUri) {}
}
