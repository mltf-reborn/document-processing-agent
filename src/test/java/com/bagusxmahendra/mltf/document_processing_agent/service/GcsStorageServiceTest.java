package com.bagusxmahendra.mltf.document_processing_agent.service;

import com.bagusxmahendra.mltf.document_processing_agent.exception.DocumentProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GcsStorageServiceTest {

    private GcsStorageService gcsStorageService;

    @BeforeEach
    void setUp() {
        gcsStorageService = new GcsStorageService();
    }

    @Test
    void testNormalizeGcsUri() {
        assertEquals("gs://my-bucket/doc.pdf", gcsStorageService.normalizeGcsUri("gs://my-bucket/doc.pdf"));
        assertEquals("gs://my-bucket/folder/subfolder/file.png", gcsStorageService.normalizeGcsUri("https://storage.googleapis.com/my-bucket/folder/subfolder/file.png"));
        assertEquals("gs://my-bucket/folder/file.jpg", gcsStorageService.normalizeGcsUri("https://storage.cloud.google.com/my-bucket/folder/file.jpg"));
    }

    @Test
    void testParseGcsReference() {
        GcsStorageService.GcsBlobReference ref = gcsStorageService.parseGcsReference("gs://my-bucket/invoices/2026/inv-001.pdf");
        assertEquals("my-bucket", ref.bucketName());
        assertEquals("invoices/2026/inv-001.pdf", ref.objectName());
        assertEquals("gs://my-bucket/invoices/2026/inv-001.pdf", ref.fullUri());
    }

    @Test
    void testParseInvalidGcsReference() {
        assertThrows(IllegalArgumentException.class, () -> gcsStorageService.parseGcsReference(""));
        assertThrows(DocumentProcessingException.class, () -> gcsStorageService.parseGcsReference("http://example.com/file.pdf"));
        assertThrows(DocumentProcessingException.class, () -> gcsStorageService.parseGcsReference("gs://onlybucket"));
    }

    @Test
    void testDetectMimeType() {
        assertEquals("application/pdf", gcsStorageService.detectMimeType("gs://b/doc.pdf", null));
        assertEquals("image/png", gcsStorageService.detectMimeType("gs://b/image.png", null));
        assertEquals("image/jpeg", gcsStorageService.detectMimeType("gs://b/photo.jpg", null));
        assertEquals("image/jpeg", gcsStorageService.detectMimeType("gs://b/photo.jpeg", null));
        assertEquals("image/webp", gcsStorageService.detectMimeType("gs://b/photo.webp", null));
        assertEquals("image/tiff", gcsStorageService.detectMimeType("gs://b/scan.tif", null));
        assertEquals("image/png", gcsStorageService.detectMimeType("gs://b/unknown_ext", "image/png"));
    }
}
