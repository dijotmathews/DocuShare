package com.dijo.client;

import com.dijo.user.Pan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validation rules for client data (UC-006 BR-01, BR-03, BR-04, BR-05).
 */
final class ClientValidator {

    static final String INVALID_EMAIL = "Please enter a valid email address.";
    static final String INVALID_CONTACT_NUMBER = "Please enter a valid contact number.";
    static final String INVALID_WHATSAPP_NUMBER = "Please enter a valid WhatsApp number.";
    static final String INVALID_PAN = "Please enter a valid PAN (5 letters, 4 digits, 1 letter, e.g. ABCDE1234F).";
    static final String FUTURE_DATE = "Date of Incorporation cannot be in the future.";
    static final String REQUIRED = "Required";
    static final int MAX_NAME_LENGTH = 200;
    static final String NAME_TOO_LONG = "Client Name must be at most " + MAX_NAME_LENGTH + " characters.";

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    // Optional leading +, then digits with optional spaces/hyphens
    private static final Pattern PHONE_CHARS = Pattern.compile("^\\+?[0-9][0-9 -]*[0-9]$");
    private static final int PHONE_MIN_DIGITS = 10;
    private static final int PHONE_MAX_DIGITS = 15;

    private ClientValidator() {
    }

    static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    static boolean isValidPhone(String phone) {
        if (phone == null || !PHONE_CHARS.matcher(phone.trim()).matches()) {
            return false;
        }
        long digits = phone.chars().filter(Character::isDigit).count();
        return digits >= PHONE_MIN_DIGITS && digits <= PHONE_MAX_DIGITS;
    }

    /**
     * Validates the data, throwing {@link ClientValidationException} if anything
     * is wrong. Missing mandatory fields are reported together (AF-2); format
     * errors are reported per field (AF-3, AF-4).
     *
     * @param checkPan false when editing, since the PAN can't be changed
     */
    static void validate(ClientDetails d, boolean checkPan, LocalDate today) {
        Map<ClientField, String> errors = new EnumMap<>(ClientField.class);
        List<String> missing = new ArrayList<>();

        requireText(d.getClientName(), ClientField.CLIENT_NAME, errors, missing);
        if (checkPan) {
            requireText(d.getPanNumber(), ClientField.PAN, errors, missing);
        }
        if (d.getDateOfIncorporation() == null) {
            errors.put(ClientField.DATE_OF_INCORPORATION, REQUIRED);
            missing.add(ClientField.DATE_OF_INCORPORATION.label());
        }
        requireText(d.getAddress(), ClientField.ADDRESS, errors, missing);
        requireText(d.getEmail(), ClientField.EMAIL, errors, missing);
        requireText(d.getContactNumber(), ClientField.CONTACT_NUMBER, errors, missing);

        if (!missing.isEmpty()) {
            throw new ClientValidationException(
                    "Please fill in the required fields: " + String.join(", ", missing) + ".", errors);
        }

        List<String> messages = new ArrayList<>();
        if (d.getClientName().trim().length() > MAX_NAME_LENGTH) {
            errors.put(ClientField.CLIENT_NAME, NAME_TOO_LONG);
            messages.add(NAME_TOO_LONG);
        }
        if (checkPan && !Pan.isValid(Pan.normalize(d.getPanNumber()))) {
            errors.put(ClientField.PAN, INVALID_PAN);
            messages.add(INVALID_PAN);
        }
        if (d.getDateOfIncorporation().isAfter(today)) {
            errors.put(ClientField.DATE_OF_INCORPORATION, FUTURE_DATE);
            messages.add(FUTURE_DATE);
        }
        if (!isValidEmail(d.getEmail())) {
            errors.put(ClientField.EMAIL, INVALID_EMAIL);
            messages.add(INVALID_EMAIL);
        }
        if (!isValidPhone(d.getContactNumber())) {
            errors.put(ClientField.CONTACT_NUMBER, INVALID_CONTACT_NUMBER);
            messages.add(INVALID_CONTACT_NUMBER);
        }
        String whatsApp = d.getWhatsAppNumber();
        if (whatsApp != null && !whatsApp.isBlank() && !isValidPhone(whatsApp)) {
            errors.put(ClientField.WHATSAPP_NUMBER, INVALID_WHATSAPP_NUMBER);
            messages.add(INVALID_WHATSAPP_NUMBER);
        }
        if (!errors.isEmpty()) {
            throw new ClientValidationException(String.join(" ", messages), errors);
        }
    }

    private static void requireText(String value, ClientField field, Map<ClientField, String> errors,
                                    List<String> missing) {
        if (value == null || value.isBlank()) {
            errors.put(field, REQUIRED);
            missing.add(field.label());
        }
    }
}
