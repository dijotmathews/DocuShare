package com.dijo.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * The default password for a client account (UC-006 BR-07): the PAN, '@' and
 * the date of incorporation as DDMMYYYY, e.g. {@code ABCDE1234F@01042015}.
 */
public final class DefaultPassword {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("ddMMyyyy");

    private DefaultPassword() {
    }

    public static String forClient(String panNumber, LocalDate dateOfIncorporation) {
        return panNumber + "@" + dateOfIncorporation.format(DATE);
    }
}
