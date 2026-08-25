package com.tcs.module.ai.service;

import com.tcs.module.ai.dto.response.AiMessageResponse;
import com.tcs.module.ai.dto.response.AiSourceResponse;
import com.tcs.module.ai.dto.response.ClassReferenceDto;
import com.tcs.module.ai.dto.response.FaqReferenceDto;
import com.tcs.module.ai.dto.response.TutorReferenceDto;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Service to build standard AiMessageResponse instances across all chat pipeline execution paths.
 */
@Service
public class AiResponseBuilderService {

    public AiMessageResponse build(
        Long messageId,
        Long sessionId,
        String content,
        String intent,
        String domain,
        String subIntent,
        String suggestedRoute,
        List<String> options,
        String answerMode,
        double score,
        String level,
        int sourceCount,
        String groundingStatus,
        List<AiSourceResponse> sources,
        List<TutorReferenceDto> tutors,
        List<ClassReferenceDto> classes,
        List<FaqReferenceDto> faqs,
        String rewrittenQuery,
        boolean isFollowUp,
        String evalNotes,
        String warningCode
    ) {
        return AiMessageResponse.builder()
            .messageId(messageId)
            .sessionId(sessionId)
            .role("assistant")
            .content(content)
            .intent(intent)
            .domain(domain)
            .subIntent(subIntent)
            .suggestedRoute(suggestedRoute)
            .clarificationOptions(options != null ? options : List.of())
            .answerMode(answerMode)
            .confidenceScore(score)
            .confidenceLevel(level)
            .sourceCount(sourceCount)
            .groundingStatus(groundingStatus)
            .sources(sources != null ? sources : List.of())
            .referencedTutors(tutors != null ? tutors : List.of())
            .referencedClasses(classes != null ? classes : List.of())
            .referencedFaqs(faqs != null ? faqs : List.of())
            .rewrittenQuery(rewrittenQuery)
            .followUp(isFollowUp)
            .evaluationNotes(evalNotes)
            .warningCode(warningCode)
            .build();
    }
}
