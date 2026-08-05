package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.dto.request.ReplyTicketRequest;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.MessagingService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportTargetType;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.entity.TicketMessage;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.TicketMessageRepository;
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
    private final PlatformAdminRepository platformAdminRepository;
    private final TicketMessageRepository ticketMessageRepository;

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
        SupportTicketPriority priority = escalatePriority(request.getCategory(), request.getPriority());
        ticket.setPriority(priority);
        ticket.setDueAt(java.time.LocalDateTime.now().plusHours(calculateSlaHours(priority)));
        ticket.setSlaBreached(false);
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

    private int calculateSlaHours(SupportTicketPriority priority) {
        if (priority == null) {
            return 48;
        }
        return switch (priority) {
            case URGENT -> 4;
            case HIGH -> 12;
            case MEDIUM -> 24;
            case LOW -> 48;
        };
    }

    private void createTicketConversation(SupportTicket ticket, User creator) {
        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(creator);
        message.setIsFromAdmin(false);
        message.setContent(ticket.getDescription());
        message.setEvidenceUrls(ticket.getEvidenceUrls());
        ticketMessageRepository.save(message);
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
                .dueAt(ticket.getDueAt())
                .slaBreached(ticket.getSlaBreached())
                .responseSlaMs(ticket.getResponseSlaMs())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportTicketDetailResponse toDetailResponse(SupportTicket ticket) {
        List<TicketMessageResponse> messages = ticketMessageRepository
                .findByTicket_TicketIdOrderByCreatedAtAsc(ticket.getTicketId())
                .stream()
                .map(msg -> toTicketMessage(msg, ticket))
                .toList();

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
                .dueAt(ticket.getDueAt())
                .slaBreached(ticket.getSlaBreached())
                .responseSlaMs(ticket.getResponseSlaMs())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .messages(messages)
                .build();
    }

    private TicketMessageResponse toTicketMessage(TicketMessage message, SupportTicket ticket) {
        return TicketMessageResponse.builder()
                .messageId(message.getMessageId())
                .senderId(message.getSender().getUserId())
                .senderName(resolveSenderName(message.getSender()))
                .fromAdmin(message.getIsFromAdmin())
                .content(message.getContent())
                .sentAt(message.getCreatedAt())
                .build();
    }

    private String resolveSenderName(User sender) {
        String email = sender.getEmail();
        return StringUtils.hasText(email) ? email : "Người dùng #" + sender.getUserId();
    }

    @Override
    @Transactional
    public TicketMessageResponse replySupportTicket(Long ticketId, ReplyTicketRequest request) {
        SupportTicket ticket = supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ"));

        if (!ticket.getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền phản hồi yêu cầu hỗ trợ này");
        }

        if (ticket.getStatus() == SupportTicketStatus.CLOSED || ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            throw new IllegalArgumentException("Không thể phản hồi yêu cầu hỗ trợ đã đóng hoặc đã giải quyết");
        }

        User sender = userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        TicketMessage message = new TicketMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setIsFromAdmin(false);
        message.setContent(request.getContent());
        message.setEvidenceUrls(request.getEvidenceUrls());

        TicketMessage saved = ticketMessageRepository.save(message);

        // Update ticket status to open if it was in review
        if (ticket.getStatus() == SupportTicketStatus.IN_REVIEW) {
            ticket.setStatus(SupportTicketStatus.OPEN);
            supportTicketRepository.save(ticket);
        }

        return toTicketMessage(saved, ticket);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse reopenSupportTicket(Long ticketId) {
        SupportTicket ticket = supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ"));

        if (!ticket.getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền mở lại yêu cầu hỗ trợ này");
        }

        if (ticket.getStatus() != SupportTicketStatus.CLOSED && ticket.getStatus() != SupportTicketStatus.RESOLVED) {
            throw new IllegalArgumentException("Yêu cầu hỗ trợ đang mở, không thể mở lại");
        }

        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticket.setDueAt(java.time.LocalDateTime.now().plusHours(calculateSlaHours(ticket.getPriority())));
        ticket.setSlaBreached(false);
        supportTicketRepository.save(ticket);

        return toDetailResponse(ticket);
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

        notifyAdminsNewReport(saved);

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

    private void notifyAdminsNewReport(Report report) {
        List<PlatformAdmin> admins = platformAdminRepository.findAll();
        if (admins.isEmpty()) {
            return;
        }
        String content = String.format(
                "Có báo cáo mới về %s (lý do: %s). Vào mục \"Nhận xét gia sư\" để kiểm tra.",
                reportTargetLabel(report.getTargetType()), reportCategoryLabel(report.getCategory()));
        for (PlatformAdmin admin : admins) {
            Notification n = new Notification();
            n.setUser(admin.getUser());
            n.setType(NotificationType.REPORT);
            n.setTitle("Báo cáo mới cần kiểm duyệt");
            n.setContent(content);
            n.setReferenceType("REPORT");
            n.setReferenceId(report.getReportId());
            n.setStatus(NotificationStatus.SENT);
            n.setIsRead(false);
            notificationRepository.save(n);
        }
    }

    private String reportTargetLabel(ReportTargetType type) {
        if (type == null) {
            return "một nội dung";
        }
        return switch (type) {
            case REVIEW -> "một nhận xét gia sư";
            case USER -> "một người dùng";
            case CLASS -> "một lớp học";
        };
    }

    private String reportCategoryLabel(ReportCategory category) {
        if (category == null) {
            return "khác";
        }
        return switch (category) {
            case FRAUD -> "sai sự thật / gian lận";
            case ABUSE -> "lăng mạ / xúc phạm";
            case INAPPROPRIATE -> "nội dung không phù hợp";
            case OTHER -> "lý do khác";
        };
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getNotificationId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .build();
    }
}
