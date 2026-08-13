package com.tcs.module.platform.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CircumventionConversationParticipantResponse {
    private Long userId;
    private String email;
}
