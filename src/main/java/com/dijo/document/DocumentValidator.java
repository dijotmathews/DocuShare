package com.dijo.document;

import com.dijo.user.Pan;

import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation rules for an upload (UC-003 BR-01, BR-03, BR-05), plus the
 * location and file name rules that keep files inside the document store.
 */
final class DocumentValidator {

    static final String REQUIRED = "Required";
    static final String INVALID_ASSESSMENT_YEAR = "Assessment Year must be a valid 4-digit year (e.g. 2025).";
    static final String INVALID_FINANCIAL_YEAR = "Financial Year must be a valid 4-digit year (e.g. 2024).";
    static final String INVALID_LOCATION = "Please enter a valid location: folder names made of letters, numbers, "
            + "spaces, '.', '-' or '_', separated by '/' (e.g. ABCDE1234F/2025).";
    static final String INVALID_FILE_NAME = "The selected file's name can't be used. Rename the file and try again.";
    static final String EMPTY_FILE = "The selected file is empty.";

    static final int MIN_YEAR = 1900;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_LOCATION_LENGTH = 1000;

    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    // Relative folders only. A segment can't start with '.' or a space, so
    // "." and ".." (leaving the document store) are impossible.
    private static final String SEGMENT = "[A-Za-z0-9_-][A-Za-z0-9 _.-]*";
    private static final Pattern LOCATION = Pattern.compile("^" + SEGMENT + "(/" + SEGMENT + ")*$");

    /** An upload that passed validation, with normalized values. */
    record ValidUpload(String panNumber, int assessmentYear, int financialYear, DocumentType documentType,
                       String location, String fileName) {
    }

    private DocumentValidator() {
    }

    /**
     * Validates the upload, throwing {@link DocumentValidationException} if
     * anything is wrong. Missing fields are reported together (AF-2); format
     * errors are reported per field.
     */
    static ValidUpload validate(DocumentUpload u, Year currentYear) {
        Map<DocumentField, String> errors = new EnumMap<>(DocumentField.class);
        List<String> missing = new ArrayList<>();

        if (u.content() == null) {
            errors.put(DocumentField.FILE, REQUIRED);
            missing.add(DocumentField.FILE.label());
        }
        requireText(u.panNumber(), DocumentField.PAN, errors, missing);
        requireText(u.assessmentYear(), DocumentField.ASSESSMENT_YEAR, errors, missing);
        requireText(u.financialYear(), DocumentField.FINANCIAL_YEAR, errors, missing);
        if (u.documentType() == null) {
            errors.put(DocumentField.DOCUMENT_TYPE, REQUIRED);
            missing.add(DocumentField.DOCUMENT_TYPE.label());
        }
        requireText(u.location(), DocumentField.LOCATION, errors, missing);

        if (!missing.isEmpty()) {
            String noun = missing.size() == 1 && errors.containsKey(DocumentField.FILE)
                    ? "Please select a file to upload."
                    : "Please fill in the required fields: " + String.join(", ", missing) + ".";
            throw new DocumentValidationException(noun, errors);
        }

        List<String> messages = new ArrayList<>();
        String fileName = cleanFileName(u.fileName());
        if (fileName == null) {
            errors.put(DocumentField.FILE, INVALID_FILE_NAME);
            messages.add(INVALID_FILE_NAME);
        } else if (u.content().length == 0) {
            errors.put(DocumentField.FILE, EMPTY_FILE);
            messages.add(EMPTY_FILE);
        }
        Integer assessmentYear = parseYear(u.assessmentYear(), currentYear);
        if (assessmentYear == null) {
            errors.put(DocumentField.ASSESSMENT_YEAR, INVALID_ASSESSMENT_YEAR);
            messages.add(INVALID_ASSESSMENT_YEAR);
        }
        Integer financialYear = parseYear(u.financialYear(), currentYear);
        if (financialYear == null) {
            errors.put(DocumentField.FINANCIAL_YEAR, INVALID_FINANCIAL_YEAR);
            messages.add(INVALID_FINANCIAL_YEAR);
        }
        String location = normalizeLocation(u.location());
        if (location == null) {
            errors.put(DocumentField.LOCATION, INVALID_LOCATION);
            messages.add(INVALID_LOCATION);
        }
        if (!errors.isEmpty()) {
            throw new DocumentValidationException(String.join(" ", messages), errors);
        }
        return new ValidUpload(Pan.normalize(u.panNumber()), assessmentYear, financialYear, u.documentType(),
                location, fileName);
    }

    /** A 4-digit year from 1900 up to next year (BR-05), or null. */
    static Integer parseYear(String value, Year currentYear) {
        String v = value.trim();
        if (!YEAR.matcher(v).matches()) {
            return null;
        }
        int year = Integer.parseInt(v);
        return year >= MIN_YEAR && year <= currentYear.getValue() + 1 ? year : null;
    }

    /** Trims and drops a trailing '/'; null if the location isn't a safe relative folder. */
    static String normalizeLocation(String value) {
        String v = value.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v.length() <= MAX_LOCATION_LENGTH && LOCATION.matcher(v).matches() ? v : null;
    }

    /** The bare file name without any client-side folders; null if nothing usable is left. */
    static String cleanFileName(String value) {
        if (value == null) {
            return null;
        }
        String name = value.substring(Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\')) + 1)
                .replaceAll("\\p{Cntrl}", "")
                .trim();
        boolean onlyDots = name.chars().allMatch(c -> c == '.');
        return name.isEmpty() || onlyDots || name.length() > MAX_FILE_NAME_LENGTH ? null : name;
    }

    private static void requireText(String value, DocumentField field, Map<DocumentField, String> errors,
                                    List<String> missing) {
        if (value == null || value.isBlank()) {
            errors.put(field, REQUIRED);
            missing.add(field.label());
        }
    }
}
