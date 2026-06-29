package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.messaging.dto.request.CreateReportRequest;
import com.tcs.module.messaging.dto.request.CreateSupportTicketRequest;
import com.tcs.module.messaging.dto.response.NotificationResponse;
import com.tcs.module.messaging.dto.response.ReportResponse;
import com.tcs.module.messaging.dto.response.SupportTicketResponse;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.messaging.service.MessagingService;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MessagingServiceImpl implements MessagingService {

    private final AuthHelper authHelper;
    private final NotificationRepository notificationRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TutoringClassRepository tutoringClassRepository;

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
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : SupportTicketPriority.MEDIUM);
        if (request.getTargetClassId() != null) {
            TutoringClass tutoringClass = tutoringClassRepository
                    .findById(request.getTargetClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
            ticket.setTargetClass(tutoringClass);
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        return SupportTicketResponse.builder()
                .ticketId(saved.getTicketId())
                .category(saved.getCategory())
                .subject(saved.getSubject())
                .priority(saved.getPriority())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
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
