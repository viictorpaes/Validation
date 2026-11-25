package com.cesar.validation.util;

import java.util.regex.Pattern;

/**
 * Utilitário de proteção de dados pessoais (LGPD).
 * Garante que CPF e endereços não sejam expostos integralmente em logs ou previews.
 */
public final class PiiMaskingUtil {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b(\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2})\\b");

    private PiiMaskingUtil() {
    }

    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return "***.***.***-**";
        }
        return digits.substring(0, 3) + ".***.***-" + digits.substring(9);
    }

    public static String sanitizeOcrPreview(String ocrText, int maxLength) {
        if (ocrText == null || ocrText.isBlank()) {
            return null;
        }
        String sanitized = CPF_PATTERN.matcher(ocrText).replaceAll("$1.***.***-$4");
        String compact = sanitized.replaceAll("\\s+", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }
}
