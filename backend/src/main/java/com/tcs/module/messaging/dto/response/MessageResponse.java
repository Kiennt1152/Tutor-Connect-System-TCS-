package com.tcs.module.messaging.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageResponse {

    private Long messageId;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isEdited;
}
