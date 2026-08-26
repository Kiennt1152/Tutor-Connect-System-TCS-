package com.tcs.module.messaging.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.exception.ForbiddenException;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.ConversationStatus;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.ConversationRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.service.CircumventionService;
import com.tcs.module.platform.service.PenaltyAccessService;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * ============================================================================
 * KIỂM THỬ TỰ ĐỘNG NHẮN TIN NHÓM THỜI GIAN THỰC (UNIT TEST GROUP CHAT)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các ca kiểm thử nhóm chat:
 *   - Tạo nhóm trò chuyện (3-20 thành viên) và gửi thông báo cho các thành viên.
 *   - Đổi tên nhóm và kiểm tra quyền Owner.
 *   - Thêm thành viên vào nhóm và kiểm tra giới hạn 20 thành viên.
 *   - Xóa thành viên khỏi nhóm và kiểm tra quyền Owner.
 *   - Chuyển quyền Trưởng nhóm (Transfer Owner) cho thành viên khác.
 *   - Rời nhóm (Leave Group) và ràng buộc Owner phải chuyển quyền trước khi rời.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplGroupTest {

    @Mock private AuthHelper authHelper;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDispatchService notificationDispatchService;
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

    @InjectMocks private ChatServiceImpl chatService;

    private final List<ConversationParticipant> participants = new ArrayList<>();
    private User owner;
    private User memberA;
    private User memberB;

    @BeforeEach
    void setUp() {
        owner = user(1L, "owner@example.com");
        memberA = user(2L, "a@example.com");
        memberB = user(3L, "b@example.com");
        when(platformMapper.resolveRole(any())).thenReturn(UserRole.CLIENT);
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(invocation -> {
            ConversationParticipant participant = invocation.getArgument(0);
            participants.add(participant);
            return participant;
        });
        when(participantRepository.findByConversation_ConversationId(anyLong()))
                .thenAnswer(invocation -> List.copyOf(participants));
        when(participantRepository.countByConversation_ConversationId(anyLong()))
                .thenAnswer(invocation -> (long) participants.size());
    }

    @Test
    @DisplayName("createGroup: tạo nhóm 3 người và gửi thông báo cho thành viên")
    void createGroup_Success() {
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(userRepository.findAllById(any())).thenReturn(List.of(memberA, memberB));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            conversation.setConversationId(10L);
            return conversation;
        });

        ConversationResponse response = chatService.createGroup(
                "Nhóm học Toán", List.of(memberA.getUserId(), memberB.getUserId()));

        assertEquals("GROUP", response.getType());
        assertEquals("Nhóm học Toán", response.getName());
        assertEquals(owner.getUserId(), response.getOwnerUserId());
        assertEquals(3, response.getParticipantCount());
        verify(notificationDispatchService, times(2)).notifyUserFromTemplate(
                any(), any(),
                org.mockito.ArgumentMatchers.eq("CHAT_GROUP_MEMBER_ADDED"),
                any(), any(), any(),
                org.mockito.ArgumentMatchers.eq("CONVERSATION"),
                org.mockito.ArgumentMatchers.eq(10L));
    }

    @Test
    @DisplayName("direct chat: giữ otherParticipant, unread count và broadcast hiện tại")
    void directChat_Regression() {
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(userRepository.findById(owner.getUserId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(memberA.getUserId())).thenReturn(Optional.of(memberA));
        when(conversationRepository.findDirectConversationBetween(
                owner.getUserId(), memberA.getUserId())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            conversation.setConversationId(20L);
            return conversation;
        });
        when(messageRepository.countByConversation_ConversationIdAndSentAtAfterAndSender_UserIdNot(
                anyLong(), any(), anyLong())).thenReturn(2L);

        ConversationResponse response = chatService.startOrGetConversation(memberA.getUserId());

        assertEquals("DIRECT", response.getType());
        assertEquals(memberA.getUserId(), response.getOtherParticipant().getUserId());
        assertEquals(2, response.getParticipantCount());
        assertEquals(2, response.getUnreadCount());

        SendMessageRequest request = new SendMessageRequest();
        request.setConversationId(response.getConversationId());
        request.setContent("Tin nhắn kiểm tra");
        when(conversationRepository.findById(response.getConversationId()))
                .thenReturn(Optional.of(participants.get(0).getConversation()));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                response.getConversationId(), owner.getUserId())).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        chatService.sendMessage(request);

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/conversation/" + response.getConversationId()),
                any(com.tcs.module.messaging.dto.response.MessageResponse.class));
    }

    @Test
    @DisplayName("createGroup: từ chối danh sách trùng lặp")
    void createGroup_RejectsDuplicateMembers() {
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());

        assertThrows(IllegalArgumentException.class, () -> chatService.createGroup(
                "Nhóm học Toán", List.of(memberA.getUserId(), memberA.getUserId())));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroup: từ chối thành viên bị khóa")
    void createGroup_RejectsInactiveMember() {
        memberB.setStatus(UserStatus.BANNED);
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(userRepository.findAllById(any())).thenReturn(List.of(memberA, memberB));

        assertThrows(IllegalArgumentException.class, () -> chatService.createGroup(
                "Nhóm học Toán", List.of(memberA.getUserId(), memberB.getUserId())));
    }

    @Test
    @DisplayName("createGroup: từ chối tên ngắn và tự thêm owner")
    void createGroup_RejectsInvalidNameAndOwnerInMemberList() {
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());

        assertThrows(IllegalArgumentException.class, () -> chatService.createGroup(
                "  ab  ", List.of(memberA.getUserId(), memberB.getUserId())));
        assertThrows(IllegalArgumentException.class, () -> chatService.createGroup(
                "Nhóm hợp lệ", List.of(owner.getUserId(), memberA.getUserId())));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createGroup: từ chối quá 19 thành viên ngoài owner")
    void createGroup_RejectsTooManyMembers() {
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        List<Long> memberIds = java.util.stream.LongStream.rangeClosed(2, 21).boxed().toList();

        assertThrows(IllegalArgumentException.class,
                () -> chatService.createGroup("Nhóm học Toán", memberIds));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("renameGroup: chỉ owner được đổi tên")
    void renameGroup_RequiresOwner() {
        Conversation group = group(owner);
        when(authHelper.currentUserId()).thenReturn(memberA.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(true);

        assertThrows(ForbiddenException.class,
                () -> chatService.renameGroup(group.getConversationId(), "Tên mới"));
    }

    @Test
    @DisplayName("transferGroupOwner: owner mới phải là participant")
    void transferGroupOwner_RequiresCurrentParticipant() {
        Conversation group = group(owner);
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), owner.getUserId())).thenReturn(true);
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> chatService.transferGroupOwner(group.getConversationId(), memberA.getUserId()));
    }

    @Test
    @DisplayName("transferGroupOwner: chuyển owner cho participant hiện tại")
    void transferGroupOwner_Success() {
        Conversation group = group(owner);
        participants.add(participant(group, owner));
        participants.add(participant(group, memberA));
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), owner.getUserId())).thenReturn(true);
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(true);
        when(userRepository.findById(memberA.getUserId())).thenReturn(Optional.of(memberA));
        when(conversationRepository.save(group)).thenReturn(group);

        ConversationResponse response = chatService.transferGroupOwner(
                group.getConversationId(), memberA.getUserId());

        assertEquals(memberA.getUserId(), response.getOwnerUserId());
    }

    @Test
    @DisplayName("addGroupMembers: thành viên mới được đọc toàn bộ lịch sử")
    void addGroupMembers_NewMemberCanReadHistory() {
        Conversation group = group(owner);
        participants.add(participant(group, owner));
        participants.add(participant(group, memberA));
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), owner.getUserId())).thenReturn(true);
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberB.getUserId())).thenReturn(false);
        when(userRepository.findAllById(any())).thenReturn(List.of(memberB));

        chatService.addGroupMembers(group.getConversationId(), List.of(memberB.getUserId()));

        when(authHelper.currentUserId()).thenReturn(memberB.getUserId());
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberB.getUserId())).thenReturn(true);
        when(messageRepository.findByConversation_ConversationIdOrderBySentAtDesc(
                anyLong(), any())).thenReturn(Page.empty());
        assertDoesNotThrow(() -> chatService.getMessages(group.getConversationId(), 0, 30));
    }

    @Test
    @DisplayName("removeGroupMember: owner xóa thành viên hiện tại")
    void removeGroupMember_Success() {
        Conversation group = group(owner);
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), owner.getUserId())).thenReturn(true);
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(true);

        chatService.removeGroupMember(group.getConversationId(), memberA.getUserId());

        verify(participantRepository).deleteByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId());
    }

    @Test
    @DisplayName("leaveGroup: thành viên rời nhóm mất quyền đọc ngay")
    void leaveGroup_RevokesAccessImmediately() {
        Conversation group = group(owner);
        when(authHelper.currentUserId()).thenReturn(memberA.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(true);

        chatService.leaveGroup(group.getConversationId());

        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), memberA.getUserId())).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> chatService.getMessages(group.getConversationId(), 0, 30));
    }

    @Test
    @DisplayName("leaveGroup: owner phải chuyển quyền trước khi rời")
    void leaveGroup_RejectsOwner() {
        Conversation group = group(owner);
        when(authHelper.currentUserId()).thenReturn(owner.getUserId());
        when(conversationRepository.findById(group.getConversationId())).thenReturn(Optional.of(group));
        when(participantRepository.existsByConversation_ConversationIdAndUser_UserId(
                group.getConversationId(), owner.getUserId())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> chatService.leaveGroup(group.getConversationId()));
        verify(participantRepository, never())
                .deleteByConversation_ConversationIdAndUser_UserId(anyLong(), anyLong());
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Conversation group(User groupOwner) {
        Conversation conversation = new Conversation();
        conversation.setConversationId(10L);
        conversation.setType("GROUP");
        conversation.setName("Nhóm học Toán");
        conversation.setOwner(groupOwner);
        conversation.setStatus(ConversationStatus.ACTIVE);
        return conversation;
    }

    private ConversationParticipant participant(Conversation conversation, User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        return participant;
    }
}
