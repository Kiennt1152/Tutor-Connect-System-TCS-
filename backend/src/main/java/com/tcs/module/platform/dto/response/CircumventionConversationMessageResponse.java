package com.tcs.module.platform.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CircumventionConversationMessageResponse {
    private Long messageId;
    private Long senderId;
    private String senderEmail;
    private String content;
    private LocalDateTime sentAt;
    private boolean flagged;
}
