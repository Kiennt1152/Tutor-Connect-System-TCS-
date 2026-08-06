package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ChatService {

    List<ConversationResponse> getMyConversations();

    ConversationResponse startOrGetConversation(Long targetUserId);

    ConversationResponse getOrCreateContextConversation(String contextType, String contextId);

    Page<MessageResponse> getMessages(Long conversationId, int page, int size);

    MessageResponse sendMessage(SendMessageRequest request);

    void markAsRead(Long conversationId);

    List<UserSummaryResponse> listUsers(String keyword);
}
