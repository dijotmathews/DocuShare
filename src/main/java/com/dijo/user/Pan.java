package com.dijo.user;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PAN (Permanent Account Number) helpers. A valid PAN is five letters, four
 * digits and one letter, e.g. {@code ABCDE1234F}.
 */
public final class Pan {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]");

    private Pan() {
    }

    /** Trims and upper-cases the input; returns an empty string for null. */
    public static String normalize(String pan) {
        return pan == null ? "" : pan.trim().toUpperCase(Locale.ROOT);
    }

    /** True if the (already normalized) value is a well-formed PAN. */
    public static boolean isValid(String pan) {
        return pan != null && FORMAT.matcher(pan).matches();
    }
}
