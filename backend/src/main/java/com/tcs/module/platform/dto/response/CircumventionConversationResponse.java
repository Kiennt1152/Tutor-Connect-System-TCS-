package com.tcs.module.platform.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CircumventionConversationResponse {
    private Long eventId;
    private Long conversationId;
    private String conversationType;
    private String conversationName;
    private Long flaggedMessageId;
    private List<CircumventionConversationParticipantResponse> participants;
    private List<CircumventionConversationMessageResponse> messages;
    private boolean hasMore;
}
