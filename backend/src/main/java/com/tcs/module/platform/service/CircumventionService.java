package com.tcs.module.platform.service;

import com.tcs.module.messaging.entity.Message;
import com.tcs.module.platform.dto.request.ReviewCircumventionRequest;
import com.tcs.module.platform.dto.response.CircumventionEventResponse;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.dto.response.PageCircumventionEventResponse;

public interface CircumventionService {
    void inspect(Message message);
    PageCircumventionEventResponse list(String status, int page, int size);
    CircumventionConversationResponse getConversationEvidence(Long eventId);
    CircumventionEventResponse review(Long eventId, ReviewCircumventionRequest request);
}
