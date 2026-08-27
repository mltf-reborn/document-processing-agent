package com.bagusxmahendra.mltf.document_processing_agent.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumentForensicToolsTest {

    private DocumentForensicTools tools;

    @BeforeEach
    void setUp() {
        tools = new DocumentForensicTools();
    }

    @Test
    void testValidateMathCalculations_Valid() {
        Map<String, Object> result = tools.validateMathCalculations("1000.00", "100.00", "50.00", "1050.00");
        assertTrue((Boolean) result.get("valid"));
        assertEquals("1050.00", result.get("calculatedTotal"));
    }

    @Test
    void testValidateMathCalculations_Discrepancy() {
        Map<String, Object> result = tools.validateMathCalculations("1000.00", "100.00", "0.00", "1500.00");
        assertFalse((Boolean) result.get("valid"));
        assertTrue(result.get("message").toString().contains("Discrepancy detected"));
    }

    @Test
    void testValidateMathCalculations_FormattedCurrencies() {
        Map<String, Object> result = tools.validateMathCalculations("$ 2,500.50", "$ 250.05", "$ 0.00", "$ 2,750.55");
        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    void testVerifyChecksum_Luhn_Valid() {
        // Standard Luhn valid: 79927398713
        Map<String, Object> result = tools.verifyChecksum("LUHN", "79927398713");
        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    void testVerifyChecksum_Luhn_Invalid() {
        // Standard Luhn invalid: 79927398714
        Map<String, Object> result = tools.verifyChecksum("LUHN", "79927398714");
        assertFalse((Boolean) result.get("valid"));
    }

    @Test
    void testValidateDateSequence_Valid() {
        Map<String, Object> result = tools.validateDateSequence("2026-01-01", "2026-01-31");
        assertTrue((Boolean) result.get("valid"));
    }

    @Test
    void testValidateDateSequence_InvalidStartAfterEnd() {
        Map<String, Object> result = tools.validateDateSequence("2026-02-15", "2026-01-10");
        assertFalse((Boolean) result.get("valid"));
        assertTrue(result.get("message").toString().contains("Start date"));
    }

    @Test
    void testValidateDateSequence_AlternateFormats() {
        Map<String, Object> result = tools.validateDateSequence("15/01/2026", "20/02/2026");
        assertTrue((Boolean) result.get("valid"));
    }
}
