package com.tcs.module.messaging.controller;

import com.tcs.module.messaging.dto.request.AddGroupMembersRequest;
import com.tcs.module.messaging.dto.request.CreateGroupRequest;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.request.StartConversationRequest;
import com.tcs.module.messaging.dto.request.TransferGroupOwnerRequest;
import com.tcs.module.messaging.dto.request.UpdateGroupRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.GroupMemberResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import com.tcs.module.messaging.service.ChatService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messaging")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public List<ConversationResponse> getMyConversations() {
        return chatService.getMyConversations();
    }

    @PostMapping("/conversations")
    public ConversationResponse startOrGetConversation(@RequestBody StartConversationRequest request) {
        return chatService.startOrGetConversation(request.getTargetUserId());
    }

    @PostMapping("/groups")
    public ConversationResponse createGroup(@RequestBody CreateGroupRequest request) {
        return chatService.createGroup(request.getName(), request.getMemberIds());
    }

    @GetMapping("/groups/{id}/members")
    public List<GroupMemberResponse> getGroupMembers(@PathVariable("id") Long conversationId) {
        return chatService.getGroupMembers(conversationId);
    }

    @PatchMapping("/groups/{id}")
    public ConversationResponse renameGroup(
            @PathVariable("id") Long conversationId, @RequestBody UpdateGroupRequest request) {
        return chatService.renameGroup(conversationId, request.getName());
    }

    @PostMapping("/groups/{id}/members")
    public ConversationResponse addGroupMembers(
            @PathVariable("id") Long conversationId, @RequestBody AddGroupMembersRequest request) {
        return chatService.addGroupMembers(conversationId, request.getMemberIds());
    }

    @DeleteMapping("/groups/{id}/members/{userId}")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable("id") Long conversationId, @PathVariable Long userId) {
        chatService.removeGroupMember(conversationId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/groups/{id}/owner")
    public ConversationResponse transferGroupOwner(
            @PathVariable("id") Long conversationId, @RequestBody TransferGroupOwnerRequest request) {
        return chatService.transferGroupOwner(conversationId, request.getOwnerUserId());
    }

    @DeleteMapping("/groups/{id}/members/me")
    public ResponseEntity<Void> leaveGroup(@PathVariable("id") Long conversationId) {
        chatService.leaveGroup(conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/context/{contextType}/{contextId}")
    public ConversationResponse getOrCreateContextConversation(
            @PathVariable("contextType") String contextType,
            @PathVariable("contextId") String contextId) {
        return chatService.getOrCreateContextConversation(contextType, contextId);
    }

    @GetMapping("/conversations/{id}/messages")
    public Page<MessageResponse> getMessages(
            @PathVariable("id") Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return chatService.getMessages(conversationId, page, size);
    }

    @PostMapping("/conversations/{id}/messages")
    public MessageResponse sendMessageViaRest(@PathVariable("id") Long conversationId, @RequestBody SendMessageRequest request) {
        request.setConversationId(conversationId);
        return chatService.sendMessage(request);
    }

    @PostMapping("/conversations/{id}/read")
    public Map<String, String> markAsRead(@PathVariable("id") Long conversationId) {
        chatService.markAsRead(conversationId);
        return Map.of("message", "Đã đánh dấu đã đọc");
    }

    @GetMapping("/users")
    public List<UserSummaryResponse> listUsers(@RequestParam(required = false) String keyword) {
        return chatService.listUsers(keyword);
    }

    /**
     * STOMP: client gui toi /app/chat.send. ChatService.sendMessage() se broadcast
     * ket qua toi /topic/conversation/{conversationId} sau khi luu DB.
     */
    @MessageMapping("/chat.send")
    public void sendMessageViaStomp(@Payload SendMessageRequest request) {
        chatService.sendMessage(request);
    }
}
