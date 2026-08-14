package com.tcs.module.ai.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class VietnameseTextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    private VietnameseTextNormalizer() {}

    /**
     * Normalize Vietnamese text: lowercase, trim, collapse spaces,
     * remove diacritics, convert đ/Đ to d/D.
     */
    public static String normalize(String text) {
        if (text == null || text.isBlank()) return "";
        String lower = text.trim().toLowerCase(Locale.ROOT);
        lower = MULTI_SPACE.matcher(lower).replaceAll(" ");
        return removeDiacritics(lower);
    }

    /**
     * Remove Vietnamese diacritics from text while preserving the base characters.
     */
    public static String removeDiacritics(String text) {
        if (text == null) return "";
        String nfd = Normalizer.normalize(text, Normalizer.Form.NFD);
        String stripped = DIACRITICS.matcher(nfd).replaceAll("");
        return stripped.replace('đ', 'd').replace('Đ', 'D');
    }
}
