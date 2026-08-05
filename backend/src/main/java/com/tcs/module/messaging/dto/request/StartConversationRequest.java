package com.tcs.module.messaging.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartConversationRequest {

    private Long targetUserId;
}
