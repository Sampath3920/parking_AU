package com.smartparking.backend.service;

import java.nio.charset.StandardCharsets;

/** Small helpers for the CSV downloads. */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** Wraps text in quotes and stops spreadsheet formula injection (=, +, -, @ at the start). */
    public static String cell(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public static byte[] bytes(String csv) {
        return csv.getBytes(StandardCharsets.UTF_8);
    }
}
