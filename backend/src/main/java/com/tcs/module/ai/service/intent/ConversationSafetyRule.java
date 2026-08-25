package com.tcs.module.ai.service.intent;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.IntentClassifier.ClassificationDetail;
import org.springframework.stereotype.Component;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.*;

@Component
public class ConversationSafetyRule implements IntentRule {

    @Override
    public int priority() {
        return 0; // Highest priority: safety & conversational tokens first
    }

    @Override
    public ClassificationDetail classify(String normalized, String lower) {
        // PRIVACY & UNAUTHORIZED DATA EXFILTRATION / ADMIN SPOOFING
        if (containsAny(normalized,
                "lay danh sach acc", "lay tat ca acc", "lay toan bo acc", "danh sach acc", "tat ca acc",
                "lay danh sach user", "lay tat ca user", "lay toan bo user", "danh sach user", "tat ca user",
                "lay danh sach tai khoan", "lay tat ca tai khoan", "lay toan bo tai khoan", "danh sach tai khoan",
                "lay danh sach nguoi dung", "lay tat ca nguoi dung", "lay toan bo nguoi dung", "danh sach nguoi dung",
                "dump database", "dump user", "dump acc", "xuat toan bo database", "lay database",
                "danh sach mat khau", "xem mat khau", "lay mat khau", "danh sach email",
                "export all users", "get all users", "list all accounts", "dump all accounts") ||
            ((containsAny(normalized, "admin", "quan tri vien") || lower.contains("admin")) &&
             (containsAny(lower, "giả sử", "giả vờ", "đóng vai", "coi như") || containsAny(normalized, "gia_dinh_hypo", "dong vai", "gia vo", "coi nhu")) &&
             containsAny(normalized, "acc", "user", "tai khoan", "nguoi dung", "database", "mat khau", "du lieu", "danh sach", "thong tin", "tat ca"))) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 1.0, "/platform/users");
        }

        // GREETING
        if (normalized.equals("xin chao") || normalized.equals("chao bot") || normalized.equals("hello") ||
            normalized.equals("hi bot") || normalized.equals("hey") || normalized.equals("alo") ||
            normalized.equals("chao em") || normalized.equals("chao anh") || normalized.equals("hi tcs") ||
            normalized.equals("chao ban") || normalized.equals("chao") || normalized.equals("hi") ||
            normalized.equals("hello tcs")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        if (normalized.startsWith("xin chao ") || normalized.startsWith("chao bot ") || normalized.startsWith("hello ")) {
            String afterGreeting = normalized;
            if (normalized.startsWith("xin chao ")) afterGreeting = normalized.substring(9).trim();
            else if (normalized.startsWith("chao bot ")) afterGreeting = normalized.substring(9).trim();
            else if (normalized.startsWith("hello ")) afterGreeting = normalized.substring(6).trim();
            afterGreeting = afterGreeting.replaceFirst("^[,!.\\s]+", "").trim();
            if (afterGreeting.isBlank() || !hasBusinessIntent(afterGreeting)) {
                return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
            }
            return null;
        }

        // GOODBYE
        if (containsAny(normalized, "tam biet", "bye", "bye bot", "hen gap lai", "bai bai", "goodbye")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GOODBYE, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // THANKS
        if (containsAny(normalized, "cam on", "thank you", "thanks", "tks", "cam on bot", "cam on nha", "cam on ban nhe")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.THANKS, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // BOT_CAPABILITY_ASK
        if (containsAny(normalized, "ban lam duoc gi", "bot lam duoc gi", "chuc nang cua bot", "huong dan su dung bot", "ban co the lam gi")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.BOT_CAPABILITY_ASK, AiIntent.FAQ_SUPPORT, 0.95, null);
        }

        // SMALL_TALK
        if (containsAny(normalized, "ban la ai", "may la ai", "who are you", "ban ten gi", "bot la ai")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.SMALL_TALK, AiIntent.OUT_OF_SCOPE, 0.95, null);
        }

        // PROFANITY_OR_FRUSTRATION
        if (hasWord(normalized, "dm") || hasWord(normalized, "vcl") || hasWord(normalized, "dit") ||
            hasWord(normalized, "clmm") || hasWord(normalized, "dmm") || hasWord(normalized, "dcm") ||
            hasWord(normalized, "dume") || hasWord(normalized, "duma") ||
            containsAny(normalized, "du me", "du ma", "dit me", "dit con me", "bot ngu", "bot nhu cac", "khon nan", "me kiep", "dm bot", "vcl")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.PROFANITY_OR_FRUSTRATION, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // HUMAN_SUPPORT_REQUEST
        if (containsAny(normalized, "gap nguoi ho tro", "gap nhan vien", "gap admin", "cham soc khach hang", "gap cskh", "gap tong dai", "cho toi gap nguoi ho tro")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.HUMAN_SUPPORT_REQUEST, AiIntent.TICKET_SUPPORT, 1.0, "/support/tickets");
        }

        // ARITHMETIC / OUT_OF_SCOPE CALCULATION & HOMEWORK
        if (containsAny(normalized,
                "giai thich cho", "the nao la", "vi du ve", "banh pizza", "pizza",
                "giai phuong trinh", "giai giup em", "giai bai toan", "giai chi tiet", "huong dan giai bai",
                "phuong trinh nay", "bai tap nay", "giai bai tap", "tinh gia tri cua", "chung minh rang") ||
            (java.util.regex.Pattern.compile("[0-9]+\\s*[+\\-*/]\\s*[0-9]+").matcher(lower).find() &&
             !containsAny(normalized, "giai bai", "bai tap", "huong dan", "phuong trinh", "dinh ly", "cong thuc"))) {
            return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.95, null);
        }

        // GIBBERISH
        if (isGibberish(normalized)) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GIBBERISH, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        return null;
    }
}
