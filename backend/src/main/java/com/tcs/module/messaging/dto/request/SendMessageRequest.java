package com.tcs.module.messaging.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Dung chung cho ca REST fallback (POST) va payload STOMP (/app/chat.send).
 */
@Getter
@Setter
public class SendMessageRequest {

    private Long conversationId;
    private String content;
}
