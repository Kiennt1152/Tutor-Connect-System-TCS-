package com.tcs.module.messaging.service;

import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.GroupMemberResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ChatService {

    List<ConversationResponse> getMyConversations();

    ConversationResponse startOrGetConversation(Long targetUserId);

    ConversationResponse createGroup(String name, List<Long> memberIds);

    List<GroupMemberResponse> getGroupMembers(Long conversationId);

    ConversationResponse renameGroup(Long conversationId, String name);

    ConversationResponse addGroupMembers(Long conversationId, List<Long> memberIds);

    void removeGroupMember(Long conversationId, Long memberUserId);

    ConversationResponse transferGroupOwner(Long conversationId, Long ownerUserId);

    void leaveGroup(Long conversationId);

    ConversationResponse getOrCreateContextConversation(String contextType, String contextId);

    Page<MessageResponse> getMessages(Long conversationId, int page, int size);

    MessageResponse sendMessage(SendMessageRequest request);

    void markAsRead(Long conversationId);

    List<UserSummaryResponse> listUsers(String keyword);
}
