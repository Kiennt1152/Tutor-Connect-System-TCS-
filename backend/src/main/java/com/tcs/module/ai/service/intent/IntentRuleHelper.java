package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared static helper methods for token matching, phrase checking, and teencode expansion.
 */
public final class IntentRuleHelper {

    private static final Set<String> VALID_ABBREVIATIONS = Set.of(
        "thptqg", "dhqghn", "khtn", "khxh", "gdcd", "thcs", "thpt",
        "hsg", "bkhn", "ftu", "hnue", "vnu", "neu", "hmu",
        "hcmus", "ussh", "uet", "bktp", "uit", "tdtu"
    );

    private IntentRuleHelper() {}

    public static boolean hasWord(String normalized, String word) {
        if (normalized == null || word == null) return false;
        String[] tokens = normalized.split("[^a-z0-9]+");
        for (String t : tokens) {
            if (t.equals(word)) return true;
        }
        return false;
    }

    public static boolean containsPhrase(String normalized, String phrase) {
        if (normalized == null || phrase == null) return false;
        String pNorm = VietnameseTextNormalizer.normalize(phrase);
        if (!pNorm.contains(" ")) {
            return hasWord(normalized, pNorm);
        }
        return normalized.contains(pNorm);
    }

    public static boolean containsAny(String normalized, String... targets) {
        if (normalized == null || targets == null) return false;
        for (String target : targets) {
            if (containsPhrase(normalized, target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGibberish(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.replaceAll("\\s+", "");
        if (VALID_ABBREVIATIONS.contains(clean.toLowerCase(Locale.ROOT))) return false;

        if (clean.length() >= 6) {
            boolean hasVowel = clean.matches(".*[aeiouy].*");
            if (!hasVowel) return true;

            if (Pattern.compile("(.)\\1{4,}").matcher(clean).find()) return true;

            if (clean.contains("asdf") || clean.contains("qwer") || clean.contains("zxcv") || clean.contains("twyalk")) return true;
        }
        return false;
    }

    public static boolean hasBusinessIntent(String text) {
        if (text == null) return false;
        String normalized = VietnameseTextNormalizer.normalize(text);
        return containsAny(normalized,
            "gia su", "gs", "lop", "tim", "can", "thue", "nap tien", "rut tien",
            "hoc phi", "mon toan", "mon ly", "mon hoa", "mon anh", "mon van",
            "ticket", "tranh chap", "khiem nai", "dang ky", "dang nhap", "doi mat khau",
            "bao nhieu nguoi dung", "thong ke", "doanh thu", "dashboard", "ho so");
    }

    public static String expandTeencode(String text) {
        if (text == null) return "";
        String result = text;
        result = result.replaceAll("\\bielst\\b", "ielts");
        result = result.replaceAll("\\btopic\\b", "topik");
        result = result.replaceAll("\\bgv\\b", "giao vien");
        result = result.replaceAll("\\bhs\\b", "hoc sinh");
        result = result.replaceAll("\\bph\\b", "phu huynh");
        result = result.replaceAll("\\bacc\\b", "tai khoan");
        result = result.replaceAll("\\bmk\\b", "mat khau");
        result = result.replaceAll("\\bpass\\b", "mat khau");
        result = result.replaceAll("\\bquen pass\\b", "quen mat khau");
        result = result.replaceAll("\\bib\\b", "nhan tin");
        result = result.replaceAll("\\binbox\\b", "nhan tin");
        result = result.replaceAll("(?<![0-9])\\bko\\b", "khong");
        result = result.replaceAll("(?<![0-9])\\bdc\\b", "duoc");
        result = result.replaceAll("\\bgiasu\\b", "gia su");
        result = result.replaceAll("\\bgs\\b", "gia su");
        result = result.replaceAll("\\bhocphi\\b", "hoc phi");
        result = result.replaceAll("\\bdaykem\\b", "day kem");
        result = result.replaceAll("\\btimlop\\b", "tim lop");
        result = result.replaceAll("(?<![0-9])\\bbn\\b", "bao nhieu");
        result = result.replaceAll("\\bvs\\b", "voi");
        result = result.replaceAll("\\bdk\\b", "dang ky");
        result = result.replaceAll("\\bdn\\b", "dang nhap");
        result = result.replaceAll("\\bbo me\\b", "phu huynh");
        return result;
    }

    public static String preprocessHypotheticals(String lower) {
        if (lower == null) return "";
        return lower
                .replaceAll("\\bgiả sử\\b", "gia_dinh_hypo")
                .replaceAll("\\bgiả thiết\\b", "gia_dinh_hypo")
                .replaceAll("\\bgiả định\\b", "gia_dinh_hypo")
                .replaceAll("\\bgiả dụ\\b", "gia_dinh_hypo")
                .replaceAll("\\bví dụ giả sử\\b", "gia_dinh_hypo");
    }

    public static String normalizeHypotheticals(String normalized) {
        if (normalized == null) return "";
        return normalized
                .replaceAll("\\bgia su\\s+(la|nhu|toi|minh|em|neu|co|ban|he thong|admin|user)\\b", "gia_dinh_hypo $1")
                .replaceAll("\\bvi du\\s+gia su\\b", "vi du gia_dinh_hypo")
                .replaceAll("\\b(gia thiet|gia dinh|gia du)\\b", "gia_dinh_hypo");
    }
}
