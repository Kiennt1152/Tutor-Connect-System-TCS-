package com.tcs.module.messaging.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConversationResponse {

    private Long conversationId;
    private String type;
    private String name;
    private Long ownerUserId;
    private int participantCount;
    private UserSummaryResponse otherParticipant;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int unreadCount;
}
