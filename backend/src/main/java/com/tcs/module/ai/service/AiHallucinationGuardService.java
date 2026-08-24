package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import com.tcs.module.ai.enums.AiDomain;
import com.tcs.module.ai.enums.AiSubIntent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to post-process LLM synthesized responses and enforce anti-hallucination policies.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiHallucinationGuardService {

    private final AiHallucinationGuard hallucinationGuard;
    private final AiFallbackService fallbackService;

    /**
     * Apply domain and entity level hallucination guards on LLM generated response.
     */
    public String applyGuards(
        String aiResponseText,
        AiDomain domain,
        AiSubIntent subIntent,
        Map<String, String> entities,
        List<TutorReferenceDto> tutors,
        List<ClassReferenceDto> classes,
        List<AiSourceResponse> allSources,
        String rawMessage,
        String userRole,
        Long userId
    ) {
        if (hallucinationGuard == null || aiResponseText == null) {
            return aiResponseText;
        }

        String noDataMsg = (fallbackService != null && fallbackService.getLevel3NoData(subIntent, entities) != null)
                ? fallbackService.getLevel3NoData(subIntent, entities).message()
                : "Hiện tại hệ thống chưa tìm thấy dữ liệu phù hợp với yêu cầu của bạn.";

        if (subIntent == AiSubIntent.FIND_TUTOR || subIntent == AiSubIntent.FILTER_TUTOR) {
            return hallucinationGuard.guardTutorResponse(aiResponseText, tutors, noDataMsg);
        } else if (subIntent == AiSubIntent.FIND_CLASS || subIntent == AiSubIntent.FILTER_CLASS) {
            return hallucinationGuard.guardClassResponse(aiResponseText, classes, noDataMsg);
        } else if (subIntent == AiSubIntent.PLATFORM_STATS) {
            return hallucinationGuard.guardStatsResponse(aiResponseText, allSources, noDataMsg);
        } else if (domain == AiDomain.FINANCE_WALLET) {
            String roleReqMsg = (fallbackService != null && fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance") != null)
                    ? fallbackService.getLevel4AuthRoleRequired("Gia sư hoặc Trung tâm gia sư", "/finance").message()
                    : "Chức năng yêu cầu quyền truy cập.";
            String financeGuardResult = hallucinationGuard.guardFinanceResponse(rawMessage, userRole, userId, roleReqMsg);
            if (financeGuardResult != null) {
                return financeGuardResult;
            }
        }

        return aiResponseText;
    }
}
