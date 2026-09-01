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
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    /** Sheet circumventionInspect - UTCID05 (B): nội dung khớp nhiều luật cùng lúc -> mỗi luật sinh một CircumventionEvent */
    @Test
    void inspect_createsOneEventPerMatchedRule() {
        User sender = new User(); sender.setUserId(7L);
        Conversation conversation = new Conversation(); conversation.setConversationId(8L);
        Message message = new Message(); message.setMessageId(9L); message.setSender(sender);
        message.setConversation(conversation); message.setContent("Liên hệ 0912345678 hoặc an@example.com");

        service.inspect(message);

        verify(repository, times(2)).save(any(CircumventionEvent.class));
    }

    /** Ngoài phạm vi Report 5.1 (MethodList không có getConversationEvidence) - test bổ sung */
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

    // ===================================================================
    //  Sheet: circumventionInspect
    //  Bon luat regex kem diem rui ro: PHONE 80, EMAIL 90, URL 70, SOCIAL 65.
    // ===================================================================
    @Nested
    @DisplayName("circumventionInspect")
    class CircumventionInspect {

        private Message messageWith(String content) {
            User sender = new User(); sender.setUserId(7L);
            Conversation conversation = new Conversation(); conversation.setConversationId(8L);
            Message m = new Message();
            m.setMessageId(9L); m.setSender(sender); m.setConversation(conversation); m.setContent(content);
            return m;
        }

        private List<CircumventionEvent> capturedEvents(int expectedCount) {
            ArgumentCaptor<CircumventionEvent> captor = ArgumentCaptor.forClass(CircumventionEvent.class);
            verify(repository, times(expectedCount)).save(captor.capture());
            return captor.getAllValues();
        }

        @Test
        @DisplayName("UTCID01 (N) - noi dung chua so dien thoai VN -> luat PHONE, diem 80")
        void utcid01_phoneDetected() {
            service.inspect(messageWith("Lien he minh qua so 0912345678 nhe"));

            CircumventionEvent e = capturedEvents(1).get(0);
            assertThat(e.getMatchedRule()).isEqualTo("PHONE");
            assertThat(e.getRiskScore()).isEqualTo(80);
            assertThat(e.getSender().getUserId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("UTCID02 (N) - noi dung chua email -> luat EMAIL, diem 90")
        void utcid02_emailDetected() {
            service.inspect(messageWith("Mail cua minh la giasu.abc@example.com"));

            CircumventionEvent e = capturedEvents(1).get(0);
            assertThat(e.getMatchedRule()).isEqualTo("EMAIL");
            assertThat(e.getRiskScore()).isEqualTo(90);
        }

        @Test
        @DisplayName("UTCID03 (N) - noi dung chua duong dan ngoai -> luat URL, diem 70")
        void utcid03_urlDetected() {
            service.inspect(messageWith("Xem tai https://example.com/lop-hoc nhe"));

            CircumventionEvent e = capturedEvents(1).get(0);
            assertThat(e.getMatchedRule()).isEqualTo("URL");
            assertThat(e.getRiskScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("UTCID04 (N) - noi dung nhac tai khoan mang xa hoi -> luat SOCIAL, diem 65")
        void utcid04_socialDetected() {
            service.inspect(messageWith("Ket ban zalo tenzalo123 di"));

            CircumventionEvent e = capturedEvents(1).get(0);
            assertThat(e.getMatchedRule()).isEqualTo("SOCIAL");
            assertThat(e.getRiskScore()).isEqualTo(65);
        }

        @Test
        @DisplayName("UTCID05 (B) - khop nhieu luat cung luc -> moi luat mot ban ghi rieng")
        void utcid05_multipleRulesMatch() {
            service.inspect(messageWith("Zalo cua minh la zalo123, mail abc@example.com"));

            assertThat(capturedEvents(2)).extracting("matchedRule")
                    .containsExactlyInAnyOrder("EMAIL", "SOCIAL");
        }

        @Test
        @DisplayName("UTCID06 (A) - message = null -> tra ve ngay, khong tao ban ghi")
        void utcid06_nullMessage() {
            service.inspect(null);
            verify(repository, never()).save(any(CircumventionEvent.class));
        }

        @Test
        @DisplayName("UTCID07 (A) - message.content = null -> tra ve ngay, khong tao ban ghi")
        void utcid07_nullContent() {
            service.inspect(messageWith(null));
            verify(repository, never()).save(any(CircumventionEvent.class));
        }

        @Test
        @DisplayName("UTCID08 (N) - noi dung sach -> khong tao ban ghi nao")
        void utcid08_cleanContent() {
            service.inspect(messageWith("Chao em, buoi hoc hom nay bat dau luc bay gio toi nhe"));
            verify(repository, never()).save(any(CircumventionEvent.class));
        }

        @Test
        @DisplayName("UTCID09 (B) - chuoi khop dai hon 500 ky tu -> evidence bi cat con dung 500")
        void utcid09_evidenceTruncatedTo500() {
            service.inspect(messageWith("https://example.com/" + "a".repeat(600)));

            assertThat(capturedEvents(1).get(0).getEvidence()).hasSize(500);
        }
    }

    private Message message(Long id, Conversation conversation, User sender, String content, LocalDateTime sentAt) {
        Message message = new Message();
        message.setMessageId(id); message.setConversation(conversation); message.setSender(sender);
        message.setContent(content); message.setSentAt(sentAt);
        return message;
    }
}
