package com.bagusxmahendra.mltf.document_processing_agent.tools;

import com.google.adk.tools.Annotations.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Domain-specific forensic verification tools for Google ADK Agent.
 * Enables the LLM Agent to invoke programmatic validation for arithmetic sums,
 * checksum integrity (e.g., Luhn algorithm), and chronological dates.
 */
@Component
public class DocumentForensicTools {

    private static final Logger log = LoggerFactory.getLogger(DocumentForensicTools.class);

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH)
    );

    /**
     * Verifies mathematical consistency between subtotal, tax, discount, and grand total.
     */
    @Schema(name = "validateMathCalculations", description = "Verifies mathematical consistency of document financial figures (subtotal + tax - discount == grandTotal) to detect altered numbers or forgery.")
    public Map<String, Object> validateMathCalculations(
            @Schema(name = "subtotal", description = "The subtotal amount stated in the document (e.g. 1200.00)") String subtotal,
            @Schema(name = "taxAmount", description = "The tax amount stated (e.g. 120.00, or 0 if none)") String taxAmount,
            @Schema(name = "discountAmount", description = "The discount amount stated (e.g. 50.00, or 0 if none)") String discountAmount,
            @Schema(name = "grandTotal", description = "The final grand total stated in the document (e.g. 1270.00)") String grandTotal
    ) {
        log.info("Executing ADK tool validateMathCalculations: subtotal={}, tax={}, discount={}, total={}", subtotal, taxAmount, discountAmount, grandTotal);
        Map<String, Object> result = new HashMap<>();

        try {
            BigDecimal sub = parseAmount(subtotal);
            BigDecimal tax = taxAmount != null && !taxAmount.isBlank() ? parseAmount(taxAmount) : BigDecimal.ZERO;
            BigDecimal disc = discountAmount != null && !discountAmount.isBlank() ? parseAmount(discountAmount) : BigDecimal.ZERO;
            BigDecimal statedTotal = parseAmount(grandTotal);

            BigDecimal calculatedTotal = sub.add(tax).subtract(disc).setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = statedTotal.subtract(calculatedTotal).abs();

            boolean isValid = diff.compareTo(new BigDecimal("0.02")) <= 0; // Allow 2 cents rounding leeway

            result.put("valid", isValid);
            result.put("statedTotal", statedTotal.toPlainString());
            result.put("calculatedTotal", calculatedTotal.toPlainString());
            result.put("difference", diff.toPlainString());
            result.put("message", isValid ? "Mathematical totals match accurately." :
                    "Discrepancy detected: Stated total is " + statedTotal + " but calculated sum is " + calculatedTotal + " (diff: " + diff + ")");
            return result;

        } catch (Exception e) {
            log.warn("Failed to validate math calculations: {}", e.getMessage());
            result.put("valid", false);
            result.put("error", "Failed to parse numeric amounts: " + e.getMessage());
            return result;
        }
    }

    /**
     * Verifies checksum algorithms (e.g. Luhn algorithm for card/account numbers).
     */
    @Schema(name = "verifyChecksum", description = "Verifies checksum algorithms like Luhn (Mod 10) for account numbers, credit cards, or ID numbers.")
    public Map<String, Object> verifyChecksum(
            @Schema(name = "type", description = "Checksum algorithm: LUHN or GENERAL_DIGIT") String type,
            @Schema(name = "value", description = "The numeric string to validate") String value
    ) {
        log.info("Executing ADK tool verifyChecksum: type={}, value={}", type, value);
        Map<String, Object> result = new HashMap<>();

        if (value == null || value.trim().isEmpty()) {
            result.put("valid", false);
            result.put("message", "Value is empty");
            return result;
        }

        String digitsOnly = value.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            result.put("valid", false);
            result.put("message", "No numeric digits found");
            return result;
        }

        if ("LUHN".equalsIgnoreCase(type)) {
            boolean validLuhn = checkLuhn(digitsOnly);
            result.put("valid", validLuhn);
            result.put("algorithm", "LUHN_MOD_10");
            result.put("digitsChecked", digitsOnly.length());
            result.put("message", validLuhn ? "Checksum verified successfully via Luhn algorithm." :
                    "Checksum failed: Number failed Luhn Mod 10 check.");
            return result;
        }

        // Generic digit count check
        result.put("valid", true);
        result.put("digitsCount", digitsOnly.length());
        result.put("message", "Numeric string contains " + digitsOnly.length() + " digits.");
        return result;
    }

    /**
     * Validates chronological order between two dates.
     */
    @Schema(name = "validateDateSequence", description = "Validates that startDate occurs on or before endDate (e.g. issueDate <= dueDate or validFrom <= validTo) to detect forged date timelines.")
    public Map<String, Object> validateDateSequence(
            @Schema(name = "startDate", description = "The earlier date string (e.g. 2026-01-01 or 01/01/2026)") String startDate,
            @Schema(name = "endDate", description = "The later date string (e.g. 2026-02-01 or 01/02/2026)") String endDate
    ) {
        log.info("Executing ADK tool validateDateSequence: start={}, end={}", startDate, endDate);
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDate start = parseDate(startDate);
            LocalDate end = parseDate(endDate);

            if (start == null || end == null) {
                result.put("valid", false);
                result.put("message", "Unable to parse one of the date strings into a recognized date format.");
                return result;
            }

            boolean isChronological = !start.isAfter(end);
            result.put("valid", isChronological);
            result.put("parsedStartDate", start.toString());
            result.put("parsedEndDate", end.toString());
            result.put("message", isChronological ? "Date sequence is chronological and valid." :
                    "Date anomaly: Start date (" + start + ") is after end date (" + end + ").");
            return result;

        } catch (Exception e) {
            log.warn("Failed to validate date sequence: {}", e.getMessage());
            result.put("valid", false);
            result.put("error", "Error parsing dates: " + e.getMessage());
            return result;
        }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null) {
            return BigDecimal.ZERO;
        }
        // Remove currency symbols, commas, spaces
        String cleaned = raw.replaceAll("[^0-9.-]", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cleaned);
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String cleaned = dateStr.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private boolean checkLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
