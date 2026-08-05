package com.tcs.module.messaging.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketMessageResponse {

    private Long messageId;
    private Long senderId;
    private String senderName;
    private boolean fromAdmin;
    private String content;
    private LocalDateTime sentAt;
}
