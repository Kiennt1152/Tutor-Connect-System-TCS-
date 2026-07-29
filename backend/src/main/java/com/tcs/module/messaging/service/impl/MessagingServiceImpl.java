package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.MessageType;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.ConversationRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.MessagingService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MessagingServiceImpl implements MessagingService {

    /** Auto-escalate: mức priority sàn theo category (BR hỗ trợ). Nếu người dùng chọn cao hơn thì giữ nguyên. */
    private static final Map<SupportTicketCategory, SupportTicketPriority> CATEGORY_MIN_PRIORITY =
            new EnumMap<>(SupportTicketCategory.class);

    static {
        CATEGORY_MIN_PRIORITY.put(SupportTicketCategory.DISPUTE, SupportTicketPriority.URGENT);
        CATEGORY_MIN_PRIORITY.put(SupportTicketCategory.SYSTEM_ERROR, SupportTicketPriority.HIGH);
        CATEGORY_MIN_PRIORITY.put(SupportTicketCategory.REPORT_USER, SupportTicketPriority.HIGH);
        CATEGORY_MIN_PRIORITY.put(SupportTicketCategory.BUG_REPORT, SupportTicketPriority.MEDIUM);
        CATEGORY_MIN_PRIORITY.put(SupportTicketCategory.INQUIRY, SupportTicketPriority.LOW);
    }

    private static final String TICKET_CONTEXT_TYPE = "SUPPORT_TICKET";

    private final AuthHelper authHelper;
    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(authHelper.currentUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
        if (!notification.getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền cập nhật thông báo này");
        }
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public SupportTicketResponse createSupportTicket(CreateSupportTicketRequest request) {
        if (request.getCategory() == null || !StringUtils.hasText(request.getSubject())) {
            throw new IllegalArgumentException("Danh mục và tiêu đề là bắt buộc");
        }
        User user = userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setCategory(request.getCategory());
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription() != null ? request.getDescription() : "");
        ticket.setEvidenceUrls(request.getEvidenceUrls());
        ticket.setPriority(escalatePriority(request.getCategory(), request.getPriority()));
        if (request.getTargetClassId() != null) {
            TutoringClass tutoringClass = tutoringClassRepository
                    .findById(request.getTargetClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
            ticket.setTargetClass(tutoringClass);
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        createTicketConversation(saved, user);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMySupportTickets() {
        return supportTicketRepository.findByUser_UserIdOrderByCreatedAtDesc(authHelper.currentUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getMySupportTicketDetail(Long ticketId) {
        SupportTicket ticket = supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ"));
        if (!ticket.getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền xem yêu cầu hỗ trợ này");
        }
        return toDetailResponse(ticket);
    }

    /** BR auto-escalate: priority cuối cùng = mức cao hơn giữa lựa chọn của người dùng và mức sàn theo category. */
    private SupportTicketPriority escalatePriority(
            SupportTicketCategory category, SupportTicketPriority requestedPriority) {
        SupportTicketPriority floor = CATEGORY_MIN_PRIORITY.getOrDefault(category, SupportTicketPriority.LOW);
        if (requestedPriority == null) {
            return floor;
        }
        return requestedPriority.ordinal() > floor.ordinal() ? requestedPriority : floor;
    }

    private void createTicketConversation(SupportTicket ticket, User creator) {
        Conversation conversation = new Conversation();
        conversation.setContextType(TICKET_CONTEXT_TYPE);
        conversation.setContextId(ticket.getTicketId());
        conversation.setType(TICKET_CONTEXT_TYPE);
        conversation.setLastMessageAt(LocalDateTime.now());
        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(savedConversation);
        participant.setUser(creator);
        conversationParticipantRepository.save(participant);

        Message message = new Message();
        message.setConversation(savedConversation);
        message.setSender(creator);
        message.setMessageType(MessageType.TEXT);
        message.setContent(ticket.getDescription());
        message.setSentAt(LocalDateTime.now());
        messageRepository.save(message);
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        return SupportTicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .targetClassId(ticket.getTargetClass() != null ? ticket.getTargetClass().getClassId() : null)
                .assignedAdminId(ticket.getAssignedAdmin() != null ? ticket.getAssignedAdmin().getAdminId() : null)
                .category(ticket.getCategory())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .evidenceUrls(ticket.getEvidenceUrls())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportTicketDetailResponse toDetailResponse(SupportTicket ticket) {
        List<TicketMessageResponse> messages = conversationRepository
                .findByContextTypeAndContextId(TICKET_CONTEXT_TYPE, ticket.getTicketId())
                .map(conversation -> messageRepository
                        .findByConversation_ConversationIdOrderBySentAtAsc(conversation.getConversationId())
                        .stream()
                        .map(msg -> toTicketMessage(msg, ticket))
                        .toList())
                .orElse(List.of());

        return SupportTicketDetailResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .targetClassId(ticket.getTargetClass() != null ? ticket.getTargetClass().getClassId() : null)
                .assignedAdminId(ticket.getAssignedAdmin() != null ? ticket.getAssignedAdmin().getAdminId() : null)
                .category(ticket.getCategory())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .evidenceUrls(ticket.getEvidenceUrls())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private TicketMessageResponse toTicketMessage(Message message, SupportTicket ticket) {
        boolean fromAdmin = ticket.getAssignedAdmin() != null
                && ticket.getAssignedAdmin().getUser().getUserId().equals(message.getSender().getUserId());
        return TicketMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserId())
                .senderName(resolveSenderName(message))
                .fromAdmin(fromAdmin)
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }

    private String resolveSenderName(Message message) {
        String email = message.getSender().getEmail();
        return StringUtils.hasText(email) ? email : "Người dùng #" + message.getSender().getUserId();
    }

    @Override
    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        if (request.getTargetType() == null || request.getTargetId() == null || request.getCategory() == null) {
            throw new IllegalArgumentException("targetType, targetId và category là bắt buộc");
        }
        User reporter = userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setCategory(request.getCategory());
        report.setDescription(request.getDescription() != null ? request.getDescription() : "");
        report.setEvidenceUrls(request.getEvidenceUrls());
        Report saved = reportRepository.save(report);
        return ReportResponse.builder()
                .reportId(saved.getReportId())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .category(saved.getCategory())
                .description(saved.getDescription())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
