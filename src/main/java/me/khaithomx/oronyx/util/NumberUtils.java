package me.khaithomx.oronyx.util;

import java.text.DecimalFormat;

/**
 * Utility class for number manipulation, validation, and formatting.
 */
public class NumberUtils {

    // --- Validation/Correction Methods ---

    /** Ensures value is 0 or positive */
    public static int ensurePositiveOrZero(int value) {
        return Math.max(0, value);
    }

    /** Ensures value is 0 or positive */
    public static long ensurePositiveOrZero(long value) {
        return Math.max(0L, value);
    }

    /** Ensures value is 0 or positive */
    public static double ensurePositiveOrZero(double value) {
        return Math.max(0.0, value);
    }

    /** Ensures value is strictly positive (>= lowerBound, usually > 0) */
    public static double ensurePositive(double value, double lowerBound) {
        return Math.max(lowerBound, value);
    }

    // --- Safe Parsing Methods ---

    /** Safely parse int, returning defaultValue on error */
    public static int safeParseInt(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.trim().replace("+", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Safely parse long, returning defaultValue on error */
    public static long safeParseLong(String s, long defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(s.trim().replace("+", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** Safely parse double, returning defaultValue on error */
    public static double safeParseDouble(String s, double defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(s.trim().replace("+", ""));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // --- Delay Parsing ---

    /**
     * Convert delay string (e.g., "60 sec", "5 min") to milliseconds.
     * Returns 60000L (60 seconds) on null/empty input or parse error.
     * Enforces a minimum delay of 1000ms (1 second).
     */
    public static long parseDelayToMillis(String delayStr) {
        if (delayStr == null || delayStr.trim().isEmpty()) {
            return 60000L;
        }
        String lowerDelay = delayStr.trim().toLowerCase();
        long multiplier = 1000L; // Default seconds
        if (lowerDelay.endsWith("ms")) {
            multiplier = 1L;
            lowerDelay = lowerDelay.substring(0, lowerDelay.length() - 2).trim();
        } else if (lowerDelay.endsWith("s") || lowerDelay.endsWith("sec")) {
            multiplier = 1000L;
            lowerDelay = lowerDelay.replaceAll("(sec|s)$", "").trim();
        } else if (lowerDelay.endsWith("m") || lowerDelay.endsWith("min")) {
            multiplier = 60 * 1000L;
            lowerDelay = lowerDelay.replaceAll("(min|m)$", "").trim();
        } else if (lowerDelay.endsWith("h") || lowerDelay.endsWith("hr")) {
            multiplier = 60 * 60 * 1000L;
            lowerDelay = lowerDelay.replaceAll("(hr|h)$", "").trim();
        }

        if (lowerDelay.isEmpty()) {
            return 60000L;
        }

        try {
            long value = Long.parseLong(lowerDelay);
            if (value < 0) {
                return 60000L;
            }
            return Math.max(1000L, value * multiplier); // Minimum delay of 1 second
        } catch (NumberFormatException e) {
            return 60000L;
        }
    }

    // --- Short Number Formatting ---

    // DecimalFormat for controlling number of decimal places
    // Shows 1 decimal place for K, M, B suffixes
    private static final DecimalFormat formatWithOneDecimal = new DecimalFormat("#,##0.0");
    // Shows no decimal places for numbers less than 1000
    private static final DecimalFormat formatNoDecimal = new DecimalFormat("#,##0");

    /**
     * Formats a number into a short representation with K (kilo), M (mega), B (giga) suffixes.
     * Uses one decimal place for K/M/B, no decimals for values below 1000.
     * Example: 1234 -> "1.2K", 500 -> "500", 1500000 -> "1.5M", 2100000000 -> "2.1B"
     *
     * @param number The number to format (accepts double).
     * @return A formatted string representation like "1.2K", "1.5M", "2.1B", or the number itself if < 1000.
     */
    public static String formatNumberShort(double number) {
        // Handle negative numbers if necessary (or decide if they should occur)
        // String sign = number < 0 ? "-" : "";
        // number = Math.abs(number);

        if (number < 1000) {
            // Less than 1000: Show as integer (no K, no decimals)
            return formatNoDecimal.format((long) number); // Cast to long to remove potential trailing .0
        } else if (number < 1_000_000) {
            // Thousands (K): Show 1 decimal place
            return formatWithOneDecimal.format(number / 1000.0) + "k";
        } else if (number < 1_000_000_000) {
            // Millions (M): Show 1 decimal place
            return formatWithOneDecimal.format(number / 1_000_000.0) + "m";
        } else {
            // Billions (B): Show 1 decimal place
            return formatWithOneDecimal.format(number / 1_000_000_000.0) + "b";
        }
        // Add T for Trillions if needed:
        // else if (number < 1_000_000_000_000L) {
        //     return formatWithOneDecimal.format(number / 1_000_000_000.0) + "B";
        // } else {
        //     return formatWithOneDecimal.format(number / 1_000_000_000_000.0) + "T";
        // }
    }

    /**
     * Overload for formatting a long number into short representation (K, M, B).
     * @param number The long number to format.
     * @return A formatted string representation.
     */
    public static String formatNumberShort(long number) {
        // Simply cast to double and call the double version
        return formatNumberShort((double) number);
    }
    // --- End Short Number Formatting ---

} // End of NumberUtils class