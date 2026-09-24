package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiIntent;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.service.intent.*;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.tcs.module.ai.service.intent.IntentRuleHelper.*;

/**
 * ============================================================================
 * [UC-65] BỘ PHÂN LOẠI Ý ĐỊNH TRUY VẤN LAI (HYBRID INTENT CLASSIFIER)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-24
 * 
 * Mô tả Use Case:
 *   - Lõi phân loại ý định người dùng kết hợp giữa hệ thống luật (Rules), so khớp vector và học tăng cường.
 * 
 * Chức năng chính:
 *   1. Phân loại đa tầng: Xác định phân hệ nghiệp vụ, ý định cụ thể và trích xuất thực thể liên quan.
 *   2. Điều hướng thông minh: Tự động cung cấp đường dẫn giao diện phù hợp với nhu cầu người dùng.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Chuẩn hóa câu hỏi (bỏ dấu tiếng Việt, mở rộng từ đồng nghĩa).
 *   - Bước 2: Chạy kiểm tra qua chuỗi IntentRuleRegistry.
 *   - Bước 3: Trả về kết quả phân loại chi tiết hoặc chuyển tiếp sang bộ phân loại dự phòng.
 * ============================================================================
 */
@Service
public class IntentClassifier {

    private final FewShotIntentClassifier fewShotIntentClassifier;
    private final IntentRuleRegistry ruleRegistry;

    public IntentClassifier() {
        this(new FewShotIntentClassifier(), createDefaultRegistry());
    }

    public IntentClassifier(FewShotIntentClassifier fewShotIntentClassifier) {
        this(fewShotIntentClassifier, createDefaultRegistry());
    }

    @Autowired
    public IntentClassifier(FewShotIntentClassifier fewShotIntentClassifier, IntentRuleRegistry ruleRegistry) {
        this.fewShotIntentClassifier = fewShotIntentClassifier != null ? fewShotIntentClassifier : new FewShotIntentClassifier();
        this.ruleRegistry = ruleRegistry != null ? ruleRegistry : createDefaultRegistry();
    }

    private static IntentRuleRegistry createDefaultRegistry() {
        return new IntentRuleRegistry(List.of(
            new ConversationSafetyRule(),
            new PlatformAdminIntentRule(),
            new MessagingTicketIntentRule(),
            new TrustSafetyIntentRule(),
            new VerificationIntentRule(),
            new ProfileGuardianIntentRule(),
            new ContractReviewIntentRule(),
            new FinanceIntentRule(),
            new IdentityAuthIntentRule(),
            new CenterOpsIntentRule(),
            new TutorOpsIntentRule(),
            new CatalogFaqIntentRule(),
            new MarketplaceIntentRule()
        ));
    }

    public record IntentResult(AiIntent intent, double confidence) {}

    public record ClassificationDetail(
        AiDomain domain,
        AiSubIntent subIntent,
        AiIntent legacyIntent,
        double confidence,
        String suggestedRoute
    ) {}

    public IntentResult classify(String message) {
        ClassificationDetail detail = classifyDetailed(message);
        return new IntentResult(detail.legacyIntent(), detail.confidence());
    }

    public ClassificationDetail checkFastPath(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        String trimmed = message.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        String normalized = VietnameseTextNormalizer.normalize(lower);
        normalized = expandTeencode(normalized);

        String[] words = normalized.split("\\s+");
        if (words.length > 4) {
            return null; // Fast path is strictly for ultra-short exact conversational tokens
        }

        // GREETING
        if (normalized.equals("xin chao") || normalized.equals("chao bot") || normalized.equals("hello") ||
            normalized.equals("hi bot") || normalized.equals("hey") || normalized.equals("alo") ||
            normalized.equals("chao em") || normalized.equals("chao anh") || normalized.equals("hi tcs") ||
            normalized.equals("chao ban") || normalized.equals("chao") || normalized.equals("hi") ||
            normalized.equals("hello tcs")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GREETING, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // GOODBYE
        if (normalized.equals("tam biet") || normalized.equals("bye") || normalized.equals("bye bot") ||
            normalized.equals("hen gap lai") || normalized.equals("bai bai") || normalized.equals("goodbye")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.GOODBYE, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // THANKS
        if (normalized.equals("cam on") || normalized.equals("thank you") || normalized.equals("thanks") ||
            normalized.equals("tks") || normalized.equals("cam on bot") || normalized.equals("cam on nha") ||
            normalized.equals("cam on ban nhe") || normalized.equals("cam on ban")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.THANKS, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // SMALL_TALK
        if (normalized.equals("ban la ai") || normalized.equals("may la ai") || normalized.equals("who are you") ||
            normalized.equals("ban ten gi") || normalized.equals("bot la ai")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.SMALL_TALK, AiIntent.OUT_OF_SCOPE, 1.0, null);
        }

        // BOT_CAPABILITY_ASK
        if (normalized.equals("ban lam duoc gi") || normalized.equals("bot lam duoc gi") ||
            normalized.equals("chuc nang cua bot") || normalized.equals("ban co the lam gi")) {
            return new ClassificationDetail(AiDomain.CONVERSATION_SAFETY, AiSubIntent.BOT_CAPABILITY_ASK, AiIntent.FAQ_SUPPORT, 1.0, null);
        }

        return null;
    }

    public ClassificationDetail classifyDetailed(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.3, null);
        }

        // Step 1: Fast path for ultra-short exact tokens
        ClassificationDetail fastPath = checkFastPath(message);
        if (fastPath != null) {
            return fastPath;
        }

        // Step 2: Query Preprocessing
        String lower = message.trim().toLowerCase(Locale.ROOT);
        String preProcessed = preprocessHypotheticals(lower);
        String normalized = VietnameseTextNormalizer.normalize(preProcessed);
        normalized = expandTeencode(normalized);
        normalized = normalizeHypotheticals(normalized);

        // Step 3: Domain Strategy Rules Evaluation
        ClassificationDetail ruleResult = ruleRegistry.evaluate(normalized, lower);
        if (ruleResult != null) {
            return ruleResult;
        }

        // Step 4: Few-Shot Exemplar Classification Fallback
        if (fewShotIntentClassifier != null) {
            Optional<FewShotIntentClassifier.FewShotMatch> fewShotOpt = fewShotIntentClassifier.classifyWithExemplars(message, 0.50);
            if (fewShotOpt.isPresent()) {
                FewShotIntentClassifier.FewShotMatch match = fewShotOpt.get();
                return new ClassificationDetail(
                    match.domain(),
                    match.subIntent(),
                    match.legacyIntent(),
                    Math.max(0.70, match.similarityScore()),
                    null
                );
            }
        }

        // Step 5: Default Out-of-Scope Fallback
        return new ClassificationDetail(AiDomain.OUT_OF_SCOPE, AiSubIntent.OUT_OF_SCOPE, AiIntent.OUT_OF_SCOPE, 0.3, null);
    }
}
