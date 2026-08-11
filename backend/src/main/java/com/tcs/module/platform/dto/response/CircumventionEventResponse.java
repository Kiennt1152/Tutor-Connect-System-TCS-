package com.tcs.module.platform.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CircumventionEventResponse {
    private Long eventId;
    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderEmail;
    private String matchedRule;
    private String evidence;
    private int riskScore;
    private String status;
    private String reviewNote;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
