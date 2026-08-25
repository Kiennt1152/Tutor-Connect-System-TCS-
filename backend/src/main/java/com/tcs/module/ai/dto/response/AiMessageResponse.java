package com.tcs.module.ai.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageResponse {
    private Long messageId;
    private Long sessionId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private List<TutorReferenceDto> referencedTutors;
    private List<ClassReferenceDto> referencedClasses;
    private List<FaqReferenceDto> referencedFaqs;
    private String intent;
    private String domain;
    private String subIntent;
    private String suggestedRoute;
    private List<String> clarificationOptions;
    private String answerMode;
    private Double confidenceScore;
    private String confidenceLevel;
    private Integer sourceCount;
    private String evaluationNotes;
    
    private String groundingStatus;
    private String warningCode;
    private String rewrittenQuery;
    private Boolean followUp;
    private List<AiSourceResponse> sources;

    public Long getMessageId() { return messageId; }
    public Long getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<TutorReferenceDto> getReferencedTutors() { return referencedTutors; }
    public List<ClassReferenceDto> getReferencedClasses() { return referencedClasses; }
    public List<FaqReferenceDto> getReferencedFaqs() { return referencedFaqs; }
    public String getIntent() { return intent; }
    public String getDomain() { return domain; }
    public String getSubIntent() { return subIntent; }
    public String getSuggestedRoute() { return suggestedRoute; }
    public List<String> getClarificationOptions() { return clarificationOptions; }
    public String getAnswerMode() { return answerMode; }
    public Double getConfidenceScore() { return confidenceScore; }
    public String getConfidenceLevel() { return confidenceLevel; }
    public Integer getSourceCount() { return sourceCount; }
    public String getEvaluationNotes() { return evaluationNotes; }
    public String getGroundingStatus() { return groundingStatus; }
    public String getWarningCode() { return warningCode; }
    public String getRewrittenQuery() { return rewrittenQuery; }
    public Boolean getFollowUp() { return followUp; }
    public List<AiSourceResponse> getSources() { return sources; }
}
