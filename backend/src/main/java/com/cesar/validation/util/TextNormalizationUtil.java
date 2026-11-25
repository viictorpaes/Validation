package com.cesar.validation.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TextNormalizationUtil {

    private static final Set<String> STOP_WORDS = Set.of("de", "da", "do", "das", "dos", "e");

    private static final List<DateTimeFormatter> SUPPORTED_DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yy")
    );

    private TextNormalizationUtil() {
    }

    public static String normalizeText(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    public static Set<String> significantNameTokens(String fullName) {
        String normalized = normalizeText(fullName);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(normalized.split(" "))
            .filter(token -> token.length() >= 2 && !STOP_WORDS.contains(token))
            .collect(Collectors.toSet());
    }

    /**
     * Calcula similaridade baseada em interseção de tokens relevantes entre nome informado e nome extraído.
     */
    public static double calculateNameSimilarity(String informedName, String extractedName) {
        Set<String> informedTokens = significantNameTokens(informedName);
        Set<String> extractedTokens = significantNameTokens(extractedName);
        if (informedTokens.isEmpty() || extractedTokens.isEmpty()) {
            return 0.0;
        }

        long matches = informedTokens.stream()
            .filter(t1 -> extractedTokens.stream().anyMatch(t2 -> t2.equals(t1) || levenshteinDistance(t1, t2) <= 1))
            .count();

        int denominator = Math.min(informedTokens.size(), extractedTokens.size());
        double minRatio = (double) matches / denominator;
        double maxRatio = (double) matches / Math.max(informedTokens.size(), extractedTokens.size());
        return (minRatio * 0.65) + (maxRatio * 0.35);
    }

    public static int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    public static String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String rawCpf) {
        String digits = digitsOnly(rawCpf);
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int sum1 = 0;
        for (int i = 0; i < 9; i++) {
            sum1 += (digits.charAt(i) - '0') * (10 - i);
        }
        int check1 = 11 - (sum1 % 11);
        if (check1 >= 10) {
            check1 = 0;
        }
        if (check1 != (digits.charAt(9) - '0')) {
            return false;
        }

        int sum2 = 0;
        for (int i = 0; i < 10; i++) {
            sum2 += (digits.charAt(i) - '0') * (11 - i);
        }
        int check2 = 11 - (sum2 % 11);
        if (check2 >= 10) {
            check2 = 0;
        }
        return check2 == (digits.charAt(10) - '0');
    }

    public static Optional<LocalDate> parseFlexibleDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return Optional.empty();
        }
        String cleaned = rawDate.trim();
        for (DateTimeFormatter formatter : SUPPORTED_DATE_FORMATS) {
            try {
                return Optional.of(LocalDate.parse(cleaned, formatter));
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return Optional.empty();
    }
}
