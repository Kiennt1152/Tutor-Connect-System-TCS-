package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.messaging.dto.request.SendMessageRequest;
import com.tcs.module.messaging.dto.response.ConversationResponse;
import com.tcs.module.messaging.dto.response.MessageResponse;
import com.tcs.module.messaging.dto.response.UserSummaryResponse;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.enums.ConversationStatus;
import com.tcs.module.messaging.enums.MessageType;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.ConversationRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.messaging.service.ChatService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.common.classrequest.ClassRequestStore;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_PREVIEW_LENGTH = 200;

    private final AuthHelper authHelper;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassRequestStore classRequestStore;
    private final PlatformMapper platformMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations() {
        Long userId = authHelper.currentUserId();
        List<Conversation> conversations = conversationRepository.findConversationsByParticipantUserId(userId);
        return conversations.stream().map(c -> toConversationResponse(c, userId)).toList();
    }

    @Override
    @Transactional
    public ConversationResponse startOrGetConversation(Long targetUserId) {
        Long userId = authHelper.currentUserId();
        if (targetUserId == null) {
            throw new IllegalArgumentException("targetUserId là bắt buộc");
        }
        if (targetUserId.equals(userId)) {
            throw new IllegalArgumentException("Không thể tự trò chuyện với chính mình");
        }
        userRepository
                .findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Conversation conversation = conversationRepository
                .findDirectConversationBetween(userId, targetUserId)
                .orElseGet(() -> createDirectConversation(userId, targetUserId));

        return toConversationResponse(conversation, userId);
    }

    @Override
    @Transactional
    public ConversationResponse getOrCreateContextConversation(String contextType, String contextIdStr) {
        Long currentUserId = authHelper.currentUserId();
        if (!StringUtils.hasText(contextType) || !StringUtils.hasText(contextIdStr)) {
            throw new IllegalArgumentException("contextType và contextId là bắt buộc");
        }

        String normalizedType = contextType.trim().toUpperCase();
        Long numericContextId = parseContextIdToLong(contextIdStr);

        Optional<Conversation> existingOpt = conversationRepository
                .findByContextTypeAndContextId(normalizedType, numericContextId);

        if (existingOpt.isPresent()) {
            Conversation conv = existingOpt.get();
            requireParticipant(conv.getConversationId(), currentUserId);
            return toConversationResponse(conv, currentUserId);
        }

        Long targetUserId = resolveOtherParticipant(normalizedType, contextIdStr, currentUserId);
        if (targetUserId == null) {
            throw new ResourceNotFoundException("Không tìm thấy đối phương cho cuộc trò chuyện này");
        }
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("Không thể tạo cuộc trò chuyện với chính mình");
        }

        Conversation conversation = new Conversation();
        conversation.setContextType(normalizedType);
        conversation.setContextId(numericContextId);
        conversation.setType(normalizedType);
        conversation.setStatus(ConversationStatus.ACTIVE);
        Conversation saved = conversationRepository.save(conversation);

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đối phương"));

        conversationParticipantRepository.save(newParticipant(saved, currentUser));
        conversationParticipantRepository.save(newParticipant(saved, targetUser));

        return toConversationResponse(saved, currentUserId);
    }

    private Long parseContextIdToLong(String contextIdStr) {
        try {
            return Long.parseLong(contextIdStr);
        } catch (NumberFormatException e) {
            return Math.abs((long) contextIdStr.hashCode());
        }
    }

    private Long resolveOtherParticipant(String contextType, String contextIdStr, Long currentUserId) {
        return switch (contextType) {
            case "APPLICATION" -> {
                Long appId = Long.parseLong(contextIdStr);
                TutorApplication app = tutorApplicationRepository.findById(appId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
                Long tutorUserId = app.getTutor().getUser().getUserId();
                Long clientUserId = app.getTutoringClass().getCreator().getUserId();
                if (!currentUserId.equals(tutorUserId) && !currentUserId.equals(clientUserId)) {
                    throw new ForbiddenException("Bạn không có quyền tham gia cuộc trò chuyện này");
                }
                yield currentUserId.equals(tutorUserId) ? clientUserId : tutorUserId;
            }
            case "RECRUITMENT", "RECRUITMENT_APPLICATION" -> {
                Long recAppId = Long.parseLong(contextIdStr);
                RecruitmentApplication app = recruitmentApplicationRepository.findById(recAppId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển trung tâm"));
                Long tutorUserId = app.getTutor().getUser().getUserId();
                Long centerUserId = app.getRecruitmentPost().getCenter().getUser().getUserId();
                if (!currentUserId.equals(tutorUserId) && !currentUserId.equals(centerUserId)) {
                    throw new ForbiddenException("Bạn không có quyền tham gia cuộc trò chuyện này");
                }
                yield currentUserId.equals(tutorUserId) ? centerUserId : tutorUserId;
            }
            case "CLASS_REQUEST" -> {
                ClassRequestStore.ClassRequestData data = classRequestStore.find(contextIdStr)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu mở lớp"));
                Long clientUserId = data.clientUserId();
                TutorCenter center = tutorCenterRepository.findById(data.centerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trung tâm"));
                Long centerUserId = center.getUser().getUserId();
                if (!currentUserId.equals(clientUserId) && !currentUserId.equals(centerUserId)) {
                    throw new ForbiddenException("Bạn không có quyền tham gia cuộc trò chuyện này");
                }
                yield currentUserId.equals(clientUserId) ? centerUserId : clientUserId;
            }
            case "CLASS_ACTIVE" -> {
                Long classId = Long.parseLong(contextIdStr);
                TutoringClass cls = tutoringClassRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
                Long creatorUserId = cls.getCreator().getUserId();
                ClassAssignment assignment = classAssignmentRepository
                        .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                        .orElseThrow(() -> new IllegalArgumentException("Lớp học chưa có gia sư được phân công"));
                Long tutorUserId = assignment.getTutor().getUser().getUserId();
                if (!currentUserId.equals(creatorUserId) && !currentUserId.equals(tutorUserId)) {
                    throw new ForbiddenException("Bạn không có quyền tham gia cuộc trò chuyện này");
                }
                yield currentUserId.equals(creatorUserId) ? tutorUserId : creatorUserId;
            }
            default -> throw new IllegalArgumentException("Loại context không hợp lệ: " + contextType);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessages(Long conversationId, int page, int size) {
        Long userId = authHelper.currentUserId();
        requireParticipant(conversationId, userId);

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 30 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Message> messages =
                messageRepository.findByConversation_ConversationIdOrderBySentAtDesc(conversationId, pageable);
        return messages.map(this::toMessageResponse);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        Long userId = authHelper.currentUserId();
        if (request.getConversationId() == null || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("conversationId và content là bắt buộc");
        }

        Conversation conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hội thoại"));
        requireParticipant(conversation.getConversationId(), userId);

        User sender = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        String content = request.getContent().trim();

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setMessageType(MessageType.TEXT);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        Message saved = messageRepository.save(message);

        conversation.setLastMessageAt(saved.getSentAt());
        conversation.setLastMessagePreview(truncatePreview(content));
        conversation.setLastMessageSenderId(userId);
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(saved);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getConversationId(), response);
        return response;
    }

    @Override
    @Transactional
    public void markAsRead(Long conversationId) {
        Long userId = authHelper.currentUserId();
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversation_ConversationIdAndUser_UserId(conversationId, userId)
                .orElseThrow(() -> new ForbiddenException("Bạn không tham gia hội thoại này"));
        participant.setLastReadAt(LocalDateTime.now());
        conversationParticipantRepository.save(participant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers(String keyword) {
        Long currentUserId = authHelper.currentUserId();
        Pageable pageable = PageRequest.of(0, 20);
        Page<User> users = userRepository.searchUsers(UserStatus.ACTIVE, keyword, pageable);
        return users.getContent().stream()
                .filter(u -> !u.getUserId().equals(currentUserId))
                .map(this::toUserSummary)
                .toList();
    }

    private Conversation createDirectConversation(Long userIdA, Long userIdB) {
        Conversation conversation = new Conversation();
        conversation.setType("DIRECT");
        conversation.setStatus(ConversationStatus.ACTIVE);
        Conversation saved = conversationRepository.save(conversation);

        User userA = userRepository
                .findById(userIdA)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        User userB = userRepository
                .findById(userIdB)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        conversationParticipantRepository.save(newParticipant(saved, userA));
        conversationParticipantRepository.save(newParticipant(saved, userB));

        return saved;
    }

    private ConversationParticipant newParticipant(Conversation conversation, User user) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        return participant;
    }

    private void requireParticipant(Long conversationId, Long userId) {
        boolean isParticipant = conversationParticipantRepository
                .existsByConversation_ConversationIdAndUser_UserId(conversationId, userId);
        if (!isParticipant) {
            throw new ForbiddenException("Bạn không tham gia hội thoại này");
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId) {
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversation_ConversationId(conversation.getConversationId());

        ConversationParticipant me = participants.stream()
                .filter(p -> p.getUser().getUserId().equals(currentUserId))
                .findFirst()
                .orElse(null);

        Optional<ConversationParticipant> otherParticipant = participants.stream()
                .filter(p -> !p.getUser().getUserId().equals(currentUserId))
                .findFirst();

        UserSummaryResponse otherSummary =
                otherParticipant.map(p -> toUserSummary(p.getUser())).orElse(null);

        LocalDateTime since = me != null ? me.getLastReadAt() : null;
        int unreadCount = since == null
                ? (int) messageRepository.countByConversation_ConversationIdAndSentAtAfterAndSender_UserIdNot(
                        conversation.getConversationId(), LocalDateTime.of(1970, 1, 1, 0, 0), currentUserId)
                : (int) messageRepository.countByConversation_ConversationIdAndSentAtAfterAndSender_UserIdNot(
                        conversation.getConversationId(), since, currentUserId);

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .type(conversation.getType())
                .otherParticipant(otherSummary)
                .lastMessagePreview(conversation.getLastMessagePreview())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .messageId(message.getMessageId())
                .conversationId(message.getConversation().getConversationId())
                .senderId(message.getSender().getUserId())
                .senderName(resolveDisplayName(message.getSender()))
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .isEdited(message.getIsEdited())
                .build();
    }

    private UserSummaryResponse toUserSummary(User user) {
        UserProfileBundle profiles = loadProfileBundle(user.getUserId());
        UserRole role = platformMapper.resolveRole(profiles);
        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .displayName(resolveDisplayName(user, profiles))
                .avatarUrl(resolveAvatarUrl(profiles))
                .role(role)
                .build();
    }

    private String resolveDisplayName(User user) {
        return resolveDisplayName(user, loadProfileBundle(user.getUserId()));
    }

    private String resolveDisplayName(User user, UserProfileBundle profiles) {
        if (profiles.platformAdmin() != null) {
            return profiles.platformAdmin().getFullName();
        }
        if (profiles.client() != null) {
            return profiles.client().getFullName();
        }
        if (profiles.tutor() != null) {
            return profiles.tutor().getFullName();
        }
        if (profiles.tutorCenter() != null) {
            return profiles.tutorCenter().getCompanyName();
        }
        return user.getEmail();
    }

    private String resolveAvatarUrl(UserProfileBundle profiles) {
        if (profiles.client() != null) {
            return profiles.client().getAvatarUrl();
        }
        if (profiles.tutor() != null) {
            return profiles.tutor().getAvatar();
        }
        if (profiles.tutorCenter() != null) {
            return profiles.tutorCenter().getAvatar();
        }
        return null;
    }

    private UserProfileBundle loadProfileBundle(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private String truncatePreview(String content) {
        if (content.length() <= MAX_PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_PREVIEW_LENGTH);
    }
}
