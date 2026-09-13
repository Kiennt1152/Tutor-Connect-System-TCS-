package com.tcs.module.messaging.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.BusinessException;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.ai.entity.AiKnowledgeChunk;
import com.tcs.module.ai.repository.AiKnowledgeChunkRepository;
import com.tcs.module.ai.service.EmbeddingService;
import com.tcs.module.ai.service.KnowledgeIndexerService;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.ConversationStatus;
import com.tcs.module.messaging.enums.MessageType;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.ConversationRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.CircumventionService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.platform.service.impl.PlatformServiceImpl;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * ============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CHUẨN HÓA CHO CÁC TEST CASE NHÓM ST-MSG (ST-MSG-001 -> ST-MSG-006)
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ST_MSG_AutomatedTestSuiteTest {

    private static final Long CLIENT_USER_ID = 10L;
    private static final Long TUTOR_USER_ID = 20L;
    private static final Long UNRELATED_USER_ID = 50L;
    private static final Long CONVERSATION_ID = 1L;
    private static final Long TICKET_ID = 100L;

    // Mocks for ChatService
    @Mock private AuthHelper authHelper;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository conversationParticipantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlatformAdminRepository platformAdminRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TutorCenterRepository tutorCenterRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private TutorApplicationRepository tutorApplicationRepository;
    @Mock private RecruitmentApplicationRepository recruitmentApplicationRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private ClassAssignmentRepository classAssignmentRepository;
    @Mock private ClassRequestStore classRequestStore;
    @Mock private PlatformMapper platformMapper;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private CircumventionService circumventionService;
    @Mock private PenaltyAccessService penaltyAccessService;
    @Mock private NotificationDispatchService notificationDispatchService;

    // Mocks for MessagingService (Ticket & SLA)
    @Mock private NotificationRepository notificationRepository;
    @Mock private SupportTicketRepository supportTicketRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;

    // Mocks for KnowledgeIndexerService (FAQ & Reindex)
    @Mock private AiKnowledgeChunkRepository chunkRepository;
    @Mock private FaqEntryRepository faqEntryRepository;
    @Mock private TutorCertificateRepository certificateRepository;
    @Mock private TutorEducationRepository educationRepository;
    @Mock private TutorExperienceRepository experienceRepository;
    @Mock private EmbeddingService embeddingService;

    private ChatServiceImpl chatService;
    private MessagingServiceImpl messagingService;
    private KnowledgeIndexerService indexerService;

    private User clientUser;
    private User tutorUser;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                authHelper, conversationRepository, conversationParticipantRepository,
                messageRepository, userRepository, platformAdminRepository, tutorRepository,
                tutorCenterRepository, clientRepository, tutorApplicationRepository,
                recruitmentApplicationRepository, tutoringClassRepository, classAssignmentRepository,
                classRequestStore, platformMapper, messagingTemplate, circumventionService,
                penaltyAccessService, notificationDispatchService);

        messagingService = new MessagingServiceImpl(
                authHelper, notificationRepository, supportTicketRepository, reportRepository,
                userRepository, tutoringClassRepository, platformAdminRepository,
                ticketMessageRepository, notificationDispatchService);

        indexerService = new KnowledgeIndexerService(
                chunkRepository, faqEntryRepository, tutorRepository,
                tutoringClassRepository, certificateRepository, educationRepository,
                experienceRepository, embeddingService, new ObjectMapper());

        clientUser = new User();
        clientUser.setUserId(CLIENT_USER_ID);
        clientUser.setEmail("client01@tcs.test");
        clientUser.setStatus(UserStatus.ACTIVE);

        tutorUser = new User();
        tutorUser.setUserId(TUTOR_USER_ID);
        tutorUser.setEmail("tutor01@tcs.test");
        tutorUser.setStatus(UserStatus.ACTIVE);

        conversation = new Conversation();
        conversation.setConversationId(CONVERSATION_ID);
        conversation.setType("DIRECT");
        conversation.setStatus(ConversationStatus.ACTIVE);
    }

    // =========================================================================
    // ST-MSG-001: Two-way realtime chat and push notifications
    // =========================================================================
    @Test
    @DisplayName("ST-MSG-001: Tin nhắn gửi đi được lưu vào CSDL và đồng bộ thời gian thực qua WebSocket STOMP topic")
    void test_ST_MSG_001_RealtimeChatWebSocketBroadcasting() {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(conversationParticipantRepository.existsByConversation_ConversationIdAndUser_UserId(CONVERSATION_ID, CLIENT_USER_ID))
                .thenReturn(true);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));

        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message m = invocation.getArgument(0);
            m.setMessageId(999L);
            m.setSentAt(LocalDateTime.now());
            return m;
        });

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(CONVERSATION_ID);
        request.setContent("Xin chào gia sư, bài tập hôm nay thế nào?");

        MessageResponse response = chatService.sendMessage(request);

        assertNotNull(response);
        assertEquals("Xin chào gia sư, bài tập hôm nay thế nào?", response.getContent());
        assertEquals(CLIENT_USER_ID, response.getSenderId());

        // Kiểm tra phát sóng thời gian thực qua WebSocket STOMP
        verify(messagingTemplate).convertAndSend(eq("/topic/conversation/" + CONVERSATION_ID), any(MessageResponse.class));
        // Kiểm tra lưu vết preview tin nhắn cuối cùng trên hội thoại
        assertEquals("Xin chào gia sư, bài tập hôm nay thế nào?", conversation.getLastMessagePreview());
    }

    // =========================================================================
    // ST-MSG-002: Create support ticket and track SLA
    // =========================================================================
    @Test
    @DisplayName("ST-MSG-002: Tạo ticket hỗ trợ kỹ thuật và tự động tính toán hạn chót cam kết chất lượng dịch vụ SLA")
    void test_ST_MSG_002_CreateSupportTicketAndSlaCalculation() {
        when(authHelper.currentUserId()).thenReturn(CLIENT_USER_ID);
        when(userRepository.findById(CLIENT_USER_ID)).thenReturn(Optional.of(clientUser));

        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket t = invocation.getArgument(0);
            t.setTicketId(TICKET_ID);
            t.setCreatedAt(LocalDateTime.now());
            return t;
        });

        CreateSupportTicketRequest request = new CreateSupportTicketRequest();
        request.setCategory(SupportTicketCategory.SYSTEM_ERROR);
        request.setSubject("Cannot top up");
        request.setDescription("Lỗi giao dịch nạp tiền qua VNPAY không cộng số dư");
        request.setPriority(SupportTicketPriority.HIGH);

        SupportTicketResponse response = messagingService.createSupportTicket(request);

        assertNotNull(response);
        assertEquals(TICKET_ID, response.getTicketId());
        assertEquals(SupportTicketPriority.HIGH, response.getPriority());
        assertNotNull(response.getDueAt(), "Ticket phải được tính toán thời hạn SLA dueAt");
        // SYSTEM_ERROR với HIGH priority được cấp hạn SLA 12 giờ
        assertTrue(response.getDueAt().isAfter(LocalDateTime.now().plusHours(11)), "Thời hạn SLA phải được cộng theo cấu hình priority");
        assertFalse(response.getSlaBreached(), "Ticket mới tạo chưa bị quá hạn");

        verify(supportTicketRepository).save(any(SupportTicket.class));
        verify(ticketMessageRepository).save(any(TicketMessage.class));
    }

    // =========================================================================
    // ST-MSG-005: Admin manages the FAQ and reindexes (Cooldown enforcement)
    // =========================================================================
    @Test
    @DisplayName("ST-MSG-005: Đánh chỉ mục tri thức lần 1 thành công; lần 2 trong khoảng thời gian cooldown bị chặn")
    void test_ST_MSG_005_AdminFaqReindexCooldownEnforced() {
        FaqEntry faq = new FaqEntry();
        faq.setFaqId(1L);
        faq.setQuestion("Làm sao nạp tiền vào ví?");
        faq.setAnswer("Vào trang Ví và chọn Nạp tiền.");
        faq.setCategory("GENERAL");
        faq.setPublished(true);

        when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc()).thenReturn(List.of(faq));
        when(tutorRepository.findAll()).thenReturn(List.of());
        when(tutoringClassRepository.findByStatus(TutoringClassStatus.OPEN)).thenReturn(List.of());
        when(embeddingService.getEmbedding(anyString())).thenReturn(Optional.of(new double[]{0.1, 0.2, 0.3}));
        when(chunkRepository.findBySourceTypeAndSourceId(any(), anyString())).thenReturn(Optional.empty());

        // Lần 1: Reindex thành công
        Map<String, Integer> firstRun = indexerService.reindexAll();
        assertNotNull(firstRun);
        assertThat(firstRun.get("indexed")).isGreaterThan(0);

        // Lần 2: Gọi ngay lập tức -> Phải kích hoạt chặn Cooldown và ném BusinessException
        BusinessException ex = assertThrows(BusinessException.class, () -> indexerService.reindexAll());
        assertTrue(ex.getMessage().contains("Vui lòng đợi"), "Thông báo phải nhắc nhở thời gian chờ cooldown: " + ex.getMessage());
    }

    // =========================================================================
    // ST-MSG-006: Accessing an unrelated conversation (Security & Privacy)
    // =========================================================================
    @Test
    @DisplayName("ST-MSG-006: Người dùng không tham gia hội thoại (tutor05) bị từ chối truy cập 100% với lỗi 403 Forbidden")
    void test_ST_MSG_006_ForbiddenAccessToUnrelatedConversation() {
        // Tutor05 (UNRELATED_USER_ID) đăng nhập
        when(authHelper.currentUserId()).thenReturn(UNRELATED_USER_ID);

        // Kiểm tra trong hội thoại bí mật CONVERSATION_ID giữa Client01 và Tutor01
        when(conversationParticipantRepository.existsByConversation_ConversationIdAndUser_UserId(
                CONVERSATION_ID, UNRELATED_USER_ID)).thenReturn(false);

        // Khi Tutor05 cố gọi lấy tin nhắn của hội thoại
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> chatService.getMessages(CONVERSATION_ID, 0, 30));

        assertEquals("Bạn không tham gia hội thoại này", ex.getMessage());
    }
}
