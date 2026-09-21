package com.example.eventday.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvUtil {

    private CsvUtil() {}

    public static String escape(String val) {
        if (val == null) return "";
        return val.contains(",") || val.contains("\"") || val.contains("\n")
                ? "\"" + val.replace("\"", "\"\"") + "\""
                : val;
    }

    public static byte[] toBytes(String csvContent) {
        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    public static void appendCsvRow(StringBuilder sb, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(fields[i]);
        }
        sb.append("\n");
    }
}
