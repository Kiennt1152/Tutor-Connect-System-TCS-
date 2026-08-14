package com.tcs.module.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AiMessageResponse {
    Long messageId;
    Long sessionId;
    String role;
    String content;
    LocalDateTime createdAt;
    List<TutorReferenceDto> referencedTutors;
    List<ClassReferenceDto> referencedClasses;
    List<FaqReferenceDto> referencedFaqs;
    String intent;
    String domain;
    String subIntent;
    String suggestedRoute;
    List<String> clarificationOptions;
    String answerMode;
    Double confidenceScore;
    String confidenceLevel;
    Integer sourceCount;
    String evaluationNotes;
    
    String groundingStatus;
    String warningCode;
    String rewrittenQuery;
    Boolean followUp;
    List<AiSourceResponse> sources;
}
