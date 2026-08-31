package com.tcs.module.messaging.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.enums.ConversationStatus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test cho {@link ChatServiceImpl#sendMessage}.
 *
 * <p>Bam bo test case trong Report_5.1_UnitTest: sheet sendMessage.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplMessageTest {

    private static final Long USER_ID = 1L;
    private static final Long CONVERSATION_ID = 10L;

    @Mock private com.tcs.security.AuthHelper authHelper;
    @Mock private com.tcs.module.messaging.repository.ConversationRepository conversationRepository;
    @Mock private com.tcs.module.messaging.repository.ConversationParticipantRepository participantRepository;
    @Mock private com.tcs.module.messaging.repository.MessageRepository messageRepository;
    @Mock private com.tcs.module.messaging.repository.NotificationRepository notificationRepository;
    @Mock private com.tcs.module.messaging.service.NotificationDispatchService notificationDispatchService;
    @Mock private com.tcs.module.identity.repository.UserRepository userRepository;
    @Mock private com.tcs.module.profile.repository.PlatformAdminRepository platformAdminRepository;
    @Mock private com.tcs.module.profile.repository.TutorRepository tutorRepository;
    @Mock private com.tcs.module.profile.repository.TutorCenterRepository tutorCenterRepository;
    @Mock private com.tcs.module.profile.repository.ClientRepository clientRepository;
    @Mock private com.tcs.module.marketplace.repository.TutorApplicationRepository tutorApplicationRepository;
    @Mock private com.tcs.module.center.repository.RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private com.tcs.module.marketplace.repository.TutoringClassRepository tutoringClassRepository;
    @Mock private com.tcs.module.marketplace.repository.ClassAssignmentRepository classAssignmentRepository;
    @Mock private com.tcs.common.classrequest.ClassRequestStore classRequestStore;
    @Mock private com.tcs.module.platform.mapper.PlatformMapper platformMapper;
    @Mock private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    @Mock private com.tcs.module.platform.service.CircumventionService circumventionService;
    @Mock private com.tcs.module.platform.service.PenaltyAccessService penaltyAccessService;

    @InjectMocks private ChatServiceImpl chatService;

    private User sender;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setUserId(USER_ID);
        sender.setEmail("nguoigui@example.com");
        sender.setStatus(UserStatus.ACTIVE);

        conversation = new Conversation();
        conversation.setConversationId(CONVERSATION_ID);
        conversation.setType("DIRECT");
        conversation.setStatus(ConversationStatus.ACTIVE);

        when(authHelper.currentUserId()).thenReturn(USER_ID);
    }

    private SendMessageRequest request(Long conversationId, String content) {
        SendMessageRequest r = new SendMessageRequest();
        r.setConversationId(conversationId);
        r.setContent(content);
        return r;
    }

    private void givenParticipant(boolean isParticipant) {
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(CONVERSATION_ID, USER_ID))
                .thenReturn(isParticipant);
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {

        @Test
        @DisplayName("UTCID01 (N) - la thanh vien, du conversationId va content -> luu tin nhan va cap nhat hoi thoai")
        void utcid01_sendSuccessfully() {
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            givenParticipant(true);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(sender));
            when(messageRepository.save(any(Message.class))).thenAnswer(i -> {
                Message m = i.getArgument(0);
                m.setMessageId(99L);
                return m;
            });

            var res = chatService.sendMessage(request(CONVERSATION_ID, "  Chao em nhe  "));

            assertEquals("Chao em nhe", res.getContent(), "noi dung phai duoc trim");
            assertEquals(CONVERSATION_ID, res.getConversationId());
            assertEquals(USER_ID, res.getSenderId());
            verify(messageRepository).save(any(Message.class));
            verify(conversationRepository).save(conversation);
            verify(circumventionService).inspect(any(Message.class));
        }

        @Test
        @DisplayName("UTCID02 (A) - thieu conversationId hoac content -> 'conversationId và content là bắt buộc'")
        void utcid02_missingFields() {
            var ex = assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(request(null, "Co noi dung")));
            assertEquals("conversationId và content là bắt buộc", ex.getMessage());

            assertThrows(IllegalArgumentException.class,
                    () -> chatService.sendMessage(request(CONVERSATION_ID, "   ")));
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("UTCID03 (A) - khong phai thanh vien hoi thoai -> 'Bạn không tham gia hội thoại này'")
        void utcid03_notParticipant() {
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
            givenParticipant(false);

            var ex = assertThrows(ForbiddenException.class,
                    () -> chatService.sendMessage(request(CONVERSATION_ID, "Chao em")));
            assertEquals("Bạn không tham gia hội thoại này", ex.getMessage());
            verify(messageRepository, never()).save(any(Message.class));
        }

        @Test
        @DisplayName("UTCID04 (A) - conversationId khong ton tai -> 'Không tìm thấy hội thoại'")
        void utcid04_conversationNotFound() {
            when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

            var ex = assertThrows(ResourceNotFoundException.class,
                    () -> chatService.sendMessage(request(CONVERSATION_ID, "Chao em")));
            assertEquals("Không tìm thấy hội thoại", ex.getMessage());
        }
    }
}
