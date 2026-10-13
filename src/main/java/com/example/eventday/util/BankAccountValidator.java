package com.example.eventday.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

public final class BankAccountValidator {

    public static final int MAX_LENGTH = 16;

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-]");
    private static final Pattern DIGITS_ONLY = Pattern.compile("\\d+");

    private BankAccountValidator() {}

    public static String clean(Object raw) {
        if (raw == null) return null;
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) return null;

        String cleaned = SEPARATORS.matcher(text).replaceAll("");

        if (cleaned.isEmpty()) {
            throw badRequest("Nomor rekening harus berisi angka");
        }
        if (!DIGITS_ONLY.matcher(cleaned).matches()) {
            throw badRequest("Nomor rekening hanya boleh berisi angka");
        }
        if (cleaned.length() > MAX_LENGTH) {
            throw badRequest("Nomor rekening maksimal " + MAX_LENGTH + " digit");
        }
        return cleaned;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
