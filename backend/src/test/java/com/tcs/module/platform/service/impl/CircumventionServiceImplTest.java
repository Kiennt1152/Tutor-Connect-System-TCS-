package com.tcs.module.platform.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.platform.dto.response.CircumventionConversationResponse;
import com.tcs.module.platform.entity.CircumventionEvent;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.security.AuthHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CircumventionServiceImplTest {
    @Mock CircumventionEventRepository repository;
    @Mock MessageRepository messageRepository;
    @Mock ConversationParticipantRepository conversationParticipantRepository;
    @Mock UserRepository userRepository;
    @Mock AuthHelper authHelper;
    @Mock AuditLogService auditLogService;
    @InjectMocks CircumventionServiceImpl service;

    @Test
    void inspect_createsOneEventPerMatchedRule() {
        User sender = new User(); sender.setUserId(7L);
        Conversation conversation = new Conversation(); conversation.setConversationId(8L);
        Message message = new Message(); message.setMessageId(9L); message.setSender(sender);
        message.setConversation(conversation); message.setContent("Liên hệ 0912345678 hoặc an@example.com");

        service.inspect(message);

        verify(repository, times(2)).save(any(CircumventionEvent.class));
    }

    @Test
    void getConversationEvidence_returnsReadOnlyContextAndFlagsDetectedMessage() {
        User sender = new User(); sender.setUserId(7L); sender.setEmail("sender@example.com");
        User recipient = new User(); recipient.setUserId(10L); recipient.setEmail("recipient@example.com");
        Conversation conversation = new Conversation(); conversation.setConversationId(8L); conversation.setType("DIRECT");
        Message newest = message(12L, conversation, recipient, "Tin moi", LocalDateTime.of(2026, 8, 12, 10, 5));
        Message flagged = message(9L, conversation, sender, "Lien he 0912345678", LocalDateTime.of(2026, 8, 12, 10, 0));
        CircumventionEvent event = new CircumventionEvent(); event.setEventId(3L); event.setConversation(conversation); event.setMessage(flagged);
        ConversationParticipant first = new ConversationParticipant(); first.setUser(sender);
        ConversationParticipant second = new ConversationParticipant(); second.setUser(recipient);

        when(repository.findById(3L)).thenReturn(Optional.of(event));
        when(messageRepository.findByConversation_ConversationIdOrderBySentAtDesc(eq(8L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(newest, flagged)));
        when(conversationParticipantRepository.findByConversation_ConversationId(8L)).thenReturn(List.of(first, second));

        CircumventionConversationResponse response = service.getConversationEvidence(3L);

        assertThat(response.getConversationId()).isEqualTo(8L);
        assertThat(response.getMessages()).extracting("messageId").containsExactly(9L, 12L);
        assertThat(response.getMessages().get(0).isFlagged()).isTrue();
        assertThat(response.getParticipants()).hasSize(2);
        verify(auditLogService).record(eq("VIEW_CIRCUMVENTION_CONVERSATION"), eq("CircumventionEvent"),
                eq(3L), eq(null), any());
    }

    private Message message(Long id, Conversation conversation, User sender, String content, LocalDateTime sentAt) {
        Message message = new Message();
        message.setMessageId(id); message.setConversation(conversation); message.setSender(sender);
        message.setContent(content); message.setSentAt(sentAt);
        return message;
    }
}
