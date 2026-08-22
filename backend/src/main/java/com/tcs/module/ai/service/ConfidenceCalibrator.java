package com.tcs.module.ai.service;

import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import com.tcs.module.ai.util.VietnameseTextNormalizer;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ConfidenceCalibrator {

    public double calibrate(double rawConfidence, AiDomain domain, AiSubIntent subIntent, Map<String, String> entities, String query) {
        if (query == null || query.isBlank()) {
            return 0.0;
        }

        double score = rawConfidence;
        String lower = query.toLowerCase(Locale.ROOT);
        String norm = VietnameseTextNormalizer.normalize(lower);

        // 1. Marketplace Intent Entity Alignment
        if (domain == AiDomain.MARKETPLACE) {
            if (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.CLASS_REGISTER_HELP) {
                boolean hasCoreEntity = entities != null && (entities.containsKey("subject") || entities.containsKey("grade") || entities.containsKey("location") || entities.containsKey("maxFee"));
                int entityCount = entities != null ? entities.size() : 0;

                if (!hasCoreEntity && score < 0.90) {
                    // Vague search without subject or grade -> reduce overconfidence
                    score = score * 0.85;
                } else if (entityCount >= 3) {
                    // Highly specific entity matches -> boost confidence
                    score = Math.min(1.0, score * 1.15);
                } else if (entityCount >= 2) {
                    score = Math.min(1.0, score * 1.08);
                }
            }
        }

        // 2. High-Certainty Keyword Grounding Boosts
        if (subIntent == AiSubIntent.PLATFORM_FEE_EXPLAIN || subIntent == AiSubIntent.ESCROW_EXPLAIN) {
            if (norm.contains("phi san") || norm.contains("phi nen tang") || norm.contains("10%") || norm.contains("escrow") || norm.contains("ky quy")) {
                score = Math.max(score, 0.98);
            }
        } else if (subIntent == AiSubIntent.WALLET_VIEW || subIntent == AiSubIntent.WITHDRAWAL_REQUEST || subIntent == AiSubIntent.WALLET_TOPUP) {
            if (norm.contains("so du") || norm.contains("vi tien") || norm.contains("rut tien") || norm.contains("nap tien") || norm.contains("vietqr") || norm.contains("sepay")) {
                score = Math.max(score, 0.96);
            }
        } else if (subIntent == AiSubIntent.TUTOR_VERIFICATION_HELP) {
            if (norm.contains("cccd") || norm.contains("xac minh") || norm.contains("bang cap") || norm.contains("duyet ho so")) {
                score = Math.max(score, 0.96);
            }
        } else if (subIntent == AiSubIntent.CONTRACT_SIGN_OTP || subIntent == AiSubIntent.CONTRACT_SIGN_HELP) {
            if (norm.contains("ky hop dong") || norm.contains("ma otp") || norm.contains("dieu khoan") || norm.contains("hop dong")) {
                score = Math.max(score, 0.95);
            }
        }

        // 3. Greeting / Goodbye / Small Talk Fast-Path Preservation
        if (domain == AiDomain.CONVERSATION_SAFETY) {
            if (subIntent == AiSubIntent.GREETING || subIntent == AiSubIntent.GOODBYE || subIntent == AiSubIntent.THANKS || subIntent == AiSubIntent.SMALL_TALK) {
                return 1.0;
            }
        }

        return Math.max(0.0, Math.min(1.0, score));
    }
}
