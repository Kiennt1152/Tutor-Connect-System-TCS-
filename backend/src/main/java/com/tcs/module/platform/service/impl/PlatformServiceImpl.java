package com.tcs.module.platform.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationHistory;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.platform.service.PlatformService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.dto.response.SupportTicketDetailResponse;
import com.tcs.module.messaging.dto.response.TicketMessageResponse;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.ConversationParticipant;
import com.tcs.module.messaging.entity.Message;
import com.tcs.module.messaging.entity.Notification;
import com.tcs.module.messaging.enums.MessageType;
import com.tcs.module.messaging.enums.NotificationStatus;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.repository.ConversationParticipantRepository;
import com.tcs.module.messaging.repository.ConversationRepository;
import com.tcs.module.messaging.repository.MessageRepository;
import com.tcs.module.messaging.repository.NotificationRepository;
import com.tcs.module.platform.dto.request.CloseTicketRequest;
import com.tcs.module.platform.dto.request.RespondTicketRequest;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.request.UpdateTicketRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.PageSupportTicketResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.SupportTicketListItemResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationDocumentResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.entity.SupportTicket;
import com.tcs.module.platform.enums.SupportTicketCategory;
import com.tcs.module.platform.enums.SupportTicketPriority;
import com.tcs.module.platform.enums.SupportTicketStatus;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private static final int MAX_PAGE_SIZE = 50;
    /** BR-03: lý do từ chối tối thiểu 10 ký tự. */
    private static final int MIN_REJECT_NOTES_LENGTH = 10;

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final ReportRepository reportRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final AuthHelper authHelper;
    private final SupportTicketRepository supportTicketRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;

    private static final String TICKET_CONTEXT_TYPE = "SUPPORT_TICKET";

    @Override
    @Transactional(readOnly = true)
    public PageUserListResponse getUsers(
            int page, int size, UserStatus status, UserRole role, String keyword) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "userId"));

        Page<User> users = queryUsers(status, role, keyword, pageable);
        List<Long> userIds = users.getContent().stream().map(User::getUserId).toList();
        ProfileMaps profileMaps = loadProfileMaps(userIds);

        List<UserListItemResponse> content = users.getContent().stream()
                .map(user -> platformMapper.toUserListItem(user, profileMaps.bundleFor(user.getUserId())))
                .sorted(Comparator.comparing(UserListItemResponse::getUserId))
                .toList();

        return PageUserListResponse.builder()
                .content(content)
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        User user = findUserOrThrow(userId);
        UserProfileBundle profiles = loadProfiles(userId);

        if (platformMapper.resolveRole(profiles) == UserRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái tài khoản quản trị viên");
        }

        UserStatus newStatus = request.getStatus();
        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.SUSPENDED
                && newStatus != UserStatus.BANNED) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }

        user.setStatus(newStatus);
        User saved = userRepository.save(user);
        return platformMapper.toUserListItem(saved, profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long pendingVerifications = verificationRequestRepository.findAll().stream()
                .filter(v -> v.getStatus() == VerificationStatus.SUBMITTED
                        || v.getStatus() == VerificationStatus.UNDER_REVIEW)
                .count();
        long openReports = reportRepository.findAll().stream()
                .filter(r -> r.getStatus() == com.tcs.module.platform.enums.ReportStatus.PENDING)
                .count();
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalTutors(tutorRepository.count())
                .totalClasses(tutoringClassRepository.count())
                .pendingVerifications(pendingVerifications)
                .openReports(openReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> listVerificationRequests() {
        return verificationRequestRepository.findAll().stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public VerificationDetailResponse getVerificationDetail(Long verificationId) {
        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        // BR-01: mở hồ sơ đang SUBMITTED sẽ tự động chuyển sang UNDER_REVIEW và ghi lịch sử.
        if (verification.getStatus() == VerificationStatus.SUBMITTED) {
            Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
            VerificationStatus oldStatus = verification.getStatus();
            verification.setStatus(VerificationStatus.UNDER_REVIEW);
            verification = verificationRequestRepository.save(verification);
            recordVerificationHistory(verification, oldStatus, VerificationStatus.UNDER_REVIEW, adminId);
        }
        return buildDetail(verification);
    }

    @Override
    @Transactional
    public VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request) {
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái xác minh không được để trống");
        }
        Long adminId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();

        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        VerificationStatus decision = request.getStatus();
        // Decision chỉ được là VERIFIED (Duyệt) hoặc REJECTED (Từ chối).
        if (decision != VerificationStatus.VERIFIED && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ. Chỉ chấp nhận Duyệt hoặc Từ chối.");
        }

        // Optimistic locking: nếu hồ sơ đã bị người khác cập nhật kể từ lúc admin mở xem,
        // chặn để tránh ghi đè quyết định của người kia (so khớp theo giây để tránh lệch nano/DB).
        LocalDateTime expectedUpdatedAt = request.getExpectedUpdatedAt();
        if (expectedUpdatedAt != null && verification.getUpdatedAt() != null
                && !verification.getUpdatedAt().truncatedTo(ChronoUnit.SECONDS)
                        .equals(expectedUpdatedAt.truncatedTo(ChronoUnit.SECONDS))) {
            throw new IllegalArgumentException(
                    "Hồ sơ vừa được cập nhật bởi người khác, vui lòng tải lại trước khi sửa.");
        }
        // Cho phép Duyệt/Từ chối khi hồ sơ đang xem xét (UNDER_REVIEW),
        // hoặc SỬA LẠI quyết định đã có (VERIFIED/REJECTED). Chỉ chặn DRAFT/SUBMITTED
        // vì phải mở hồ sơ để xem xét trước (BR-01 chuyển SUBMITTED -> UNDER_REVIEW).
        VerificationStatus current = verification.getStatus();
        if (current != VerificationStatus.UNDER_REVIEW
                && current != VerificationStatus.VERIFIED
                && current != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException(
                    "Hồ sơ chưa sẵn sàng để duyệt. Vui lòng mở hồ sơ để xem xét trước.");
        }
        // BR-03 / AF-01: khi Từ chối bắt buộc nhập lý do (>= 10 ký tự).
        if (decision == VerificationStatus.REJECTED) {
            String notes = request.getAdminNotes() == null ? "" : request.getAdminNotes().trim();
            if (notes.length() < MIN_REJECT_NOTES_LENGTH) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).");
            }
        }

        VerificationStatus oldStatus = verification.getStatus();
        verification.setStatus(request.getStatus());
        verification.setAdminNotes(request.getAdminNotes());
        verification.setReviewedBy(adminId);
        verification.setReviewedAt(java.time.LocalDateTime.now());
        if (request.getStatus() == VerificationStatus.REJECTED) {
            verification.setRejectionReason(request.getAdminNotes());
        } else {
            verification.setRejectionReason(null);
        }
        VerificationRequest saved = verificationRequestRepository.save(verification);

        recordVerificationHistory(saved, oldStatus, request.getStatus(), adminId);
        if (request.getStatus() == VerificationStatus.VERIFIED
                || request.getStatus() == VerificationStatus.REJECTED) {
            ProfileVerificationStatus profileStatus = request.getStatus() == VerificationStatus.VERIFIED
                    ? ProfileVerificationStatus.VERIFIED
                    : ProfileVerificationStatus.REJECTED;
            Long userId = saved.getUser().getUserId();
            tutorRepository.findByUser_UserId(userId).ifPresent(tutor -> {
                tutor.setVerificationStatus(profileStatus);
                tutorRepository.save(tutor);
            });
            tutorCenterRepository.findByUser_UserId(userId).ifPresent(center -> {
                center.setVerificationStatus(profileStatus);
                tutorCenterRepository.save(center);
            });
            sendVerificationNotification(saved, request.getStatus());
        }
        return toVerificationResponse(saved);
    }

    private void recordVerificationHistory(VerificationRequest request,
                                           VerificationStatus oldStatus,
                                           VerificationStatus newStatus,
                                           Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found: " + adminId));
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(request);
        history.setOldStatus(oldStatus != null ? oldStatus.name() : null);
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(admin);
        verificationHistoryRepository.save(history);
    }

    private void sendVerificationNotification(VerificationRequest request, VerificationStatus status) {
        String title;
        String content;
        if (status == VerificationStatus.VERIFIED) {
            title = "Hồ sơ xác minh được duyệt";
            content = "Hồ sơ xác minh của bạn đã được duyệt.";
        } else if (status == VerificationStatus.REJECTED) {
            title = "Hồ sơ xác minh bị từ chối";
            content = "Lý do: "
                    + (request.getRejectionReason() != null ? request.getRejectionReason() : "không rõ");
        } else {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(request.getUser());
        notification.setType(NotificationType.VERIFICATION);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType("VERIFICATION_REQUEST");
        notification.setReferenceId(request.getVerificationId());
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private VerificationDetailResponse buildDetail(VerificationRequest v) {
        Long userId = v.getUser().getUserId();
        Map<String, String> details = new LinkedHashMap<>();
        String submitterName = null;
        String submitterPhone = v.getUser().getPhone();

        if (v.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
            if (tutor != null) {
                submitterName = tutor.getFullName();
                if (StringUtils.hasText(tutor.getPhone())) {
                    submitterPhone = tutor.getPhone();
                }
                details.put("Giới tính", tutor.getGender() == null ? "—" : tutor.getGender().name());
                details.put("Số năm kinh nghiệm", String.valueOf(tutor.getExperienceYears()));
                details.put("Địa chỉ", orDash(tutor.getAddress()));
                details.put("Giới thiệu", orDash(tutor.getBio()));
                details.put("Trạng thái xác minh", tutor.getVerificationStatus().name());
            }
        } else {
            TutorCenter center = tutorCenterRepository.findByUser_UserId(userId).orElse(null);
            if (center != null) {
                submitterName = center.getCompanyName();
                if (StringUtils.hasText(center.getPhone())) {
                    submitterPhone = center.getPhone();
                }
                details.put("Số giấy phép", orDash(center.getLicenseNo()));
                details.put("Địa chỉ", orDash(center.getAddress()));
                details.put("Mô tả", orDash(center.getDescription()));
                details.put("Trạng thái xác minh", center.getVerificationStatus().name());
            }
        }

        List<VerificationDocumentResponse> documents = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(v.getVerificationId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        boolean hasUnreadable = documents.stream().anyMatch(doc -> !doc.isAvailable());

        return VerificationDetailResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(userId)
                .userEmail(v.getUser().getEmail())
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .submitterName(submitterName)
                .submitterPhone(submitterPhone)
                .submitterDetails(details)
                .documents(documents)
                .hasUnreadableDocument(hasUnreadable)
                .build();
    }

    private VerificationDocumentResponse toDocumentResponse(VerificationDocument doc) {
        MediaFile file = doc.getFile();
        boolean available = file != null && StringUtils.hasText(file.getFileUrl());
        return VerificationDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .documentType(doc.getDocumentType())
                .fileId(file == null ? null : file.getFileId())
                .fileName(file == null ? null : file.getFileName())
                .fileUrl(file == null ? null : file.getFileUrl())
                .mimeType(file == null ? null : file.getMimeType())
                .fileSize(file == null ? null : file.getFileSize())
                .available(available)
                .build();
    }

    private String orDash(String value) {
        return StringUtils.hasText(value) ? value : "—";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listReports() {
        return reportRepository.findAll().stream().map(this::toReportResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageSupportTicketResponse getTickets(
            int page, int size, SupportTicketStatus status,
            SupportTicketCategory category, SupportTicketPriority priority, String keyword) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        Page<SupportTicket> tickets =
                supportTicketRepository.search(status, category, priority, trimmedKeyword, pageable);

        List<SupportTicketListItemResponse> content =
                tickets.getContent().stream().map(this::toTicketListItem).toList();

        return PageSupportTicketResponse.builder()
                .content(content)
                .page(tickets.getNumber())
                .size(tickets.getSize())
                .totalElements(tickets.getTotalElements())
                .totalPages(tickets.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse getTicketDetail(Long ticketId) {
        SupportTicket ticket = findTicketOrThrow(ticketId);

        // Mở ticket lần đầu: tự động gán admin hiện tại và chuyển OPEN -> IN_PROGRESS.
        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            PlatformAdmin admin = currentAdminOrThrow();
            ticket.setAssignedAdmin(admin);
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            ticket = supportTicketRepository.save(ticket);
        }
        return toTicketDetail(ticket);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        SupportTicket ticket = findTicketOrThrow(ticketId);

        if (request.getCategory() != null) {
            ticket.setCategory(request.getCategory());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        SupportTicket saved = supportTicketRepository.save(ticket);
        return toTicketDetail(saved);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse respondToTicket(Long ticketId, RespondTicketRequest request) {
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("Nội dung phản hồi là bắt buộc");
        }
        SupportTicket ticket = findTicketOrThrow(ticketId);
        PlatformAdmin admin = currentAdminOrThrow();

        if (ticket.getAssignedAdmin() == null) {
            ticket.setAssignedAdmin(admin);
        }

        Conversation conversation = getOrCreateTicketConversation(ticket, ticket.getUser());
        ensureParticipant(conversation, admin.getUser());

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(admin.getUser());
        message.setMessageType(MessageType.TEXT);
        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        messageRepository.save(message);

        conversation.setLastMessageAt(message.getSentAt());
        conversationRepository.save(conversation);

        if (ticket.getStatus() != SupportTicketStatus.RESOLVED && ticket.getStatus() != SupportTicketStatus.CLOSED) {
            ticket.setStatus(SupportTicketStatus.IN_REVIEW);
        }
        SupportTicket saved = supportTicketRepository.save(ticket);

        notifyUserOfTicketResponse(saved, request.getContent());
        return toTicketDetail(saved);
    }

    @Override
    @Transactional
    public SupportTicketDetailResponse closeTicket(Long ticketId, CloseTicketRequest request) {
        if (request.getStatus() != SupportTicketStatus.RESOLVED && request.getStatus() != SupportTicketStatus.CLOSED) {
            throw new IllegalArgumentException("Chỉ chấp nhận trạng thái RESOLVED hoặc CLOSED");
        }
        SupportTicket ticket = findTicketOrThrow(ticketId);

        ticket.setStatus(request.getStatus());
        LocalDateTime now = LocalDateTime.now();
        if (request.getStatus() == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(now);
        } else {
            ticket.setClosedAt(now);
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(now);
            }
        }
        SupportTicket saved = supportTicketRepository.save(ticket);

        if (StringUtils.hasText(request.getAdminNotes())) {
            notifyUserOfTicketResponse(saved, request.getAdminNotes());
        }
        return toTicketDetail(saved);
    }

    private SupportTicket findTicketOrThrow(Long ticketId) {
        return supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hỗ trợ"));
    }

    private PlatformAdmin currentAdminOrThrow() {
        Long adminUserId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        return platformAdminRepository
                .findByUser_UserId(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
    }

    private Conversation getOrCreateTicketConversation(SupportTicket ticket, User creator) {
        return conversationRepository
                .findByContextTypeAndContextId(TICKET_CONTEXT_TYPE, ticket.getTicketId())
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setContextType(TICKET_CONTEXT_TYPE);
                    conversation.setContextId(ticket.getTicketId());
                    conversation.setType(TICKET_CONTEXT_TYPE);
                    conversation.setLastMessageAt(LocalDateTime.now());
                    Conversation saved = conversationRepository.save(conversation);
                    ensureParticipant(saved, creator);
                    return saved;
                });
    }

    private void ensureParticipant(Conversation conversation, User user) {
        boolean alreadyParticipant = conversationParticipantRepository.findAll().stream()
                .anyMatch(p -> p.getConversation().getConversationId().equals(conversation.getConversationId())
                        && p.getUser().getUserId().equals(user.getUserId()));
        if (alreadyParticipant) {
            return;
        }
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        conversationParticipantRepository.save(participant);
    }

    private void notifyUserOfTicketResponse(SupportTicket ticket, String content) {
        Notification notification = new Notification();
        notification.setUser(ticket.getUser());
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("Phản hồi yêu cầu hỗ trợ #" + ticket.getTicketId());
        notification.setContent(content);
        notification.setReferenceType("SUPPORT_TICKET");
        notification.setReferenceId(ticket.getTicketId());
        notification.setStatus(NotificationStatus.SENT);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    private SupportTicketListItemResponse toTicketListItem(SupportTicket ticket) {
        PlatformAdmin admin = ticket.getAssignedAdmin();
        return SupportTicketListItemResponse.builder()
                .ticketId(ticket.getTicketId())
                .userId(ticket.getUser().getUserId())
                .userEmail(ticket.getUser().getEmail())
                .assignedAdminId(admin != null ? admin.getAdminId() : null)
                .assignedAdminName(admin != null ? admin.getFullName() : null)
                .category(ticket.getCategory())
                .subject(ticket.getSubject())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    private SupportTicketDetailResponse toTicketDetail(SupportTicket ticket) {
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

    private VerificationRequestResponse toVerificationResponse(VerificationRequest v) {
        return VerificationRequestResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(v.getUser().getUserId())
                .userEmail(v.getUser().getEmail())
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .build();
    }

    private ReportResponse toReportResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporter().getUserId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .category(report.getCategory())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private Page<User> queryUsers(UserStatus status, UserRole role, String keyword, PageRequest pageable) {
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if (role != null && role != UserRole.UNKNOWN) {
            List<Long> roleUserIds = findUserIdsByRole(role);
            if (roleUserIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return userRepository.searchUsersByIds(roleUserIds, status, trimmedKeyword, pageable);
        }

        return userRepository.searchUsers(status, trimmedKeyword, pageable);
    }

    private List<Long> findUserIdsByRole(UserRole role) {
        return switch (role) {
            case PLATFORM_ADMIN -> platformAdminRepository.findAll().stream()
                    .map(admin -> admin.getUser().getUserId())
                    .toList();
            case TUTOR -> tutorRepository.findAll().stream()
                    .map(tutor -> tutor.getUser().getUserId())
                    .toList();
            case TUTOR_CENTER -> tutorCenterRepository.findAll().stream()
                    .map(center -> center.getUser().getUserId())
                    .toList();
            case CLIENT -> clientRepository.findAll().stream()
                    .map(client -> client.getUser().getUserId())
                    .toList();
            default -> List.of();
        };
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private ProfileMaps loadProfileMaps(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return ProfileMaps.empty();
        }

        Map<Long, PlatformAdmin> adminMap = toMapByUserId(
                platformAdminRepository.findByUser_UserIdIn(userIds), PlatformAdmin::getUser);
        Map<Long, Tutor> tutorMap =
                toMapByUserId(tutorRepository.findByUser_UserIdIn(userIds), Tutor::getUser);
        Map<Long, TutorCenter> centerMap = toMapByUserId(
                tutorCenterRepository.findByUser_UserIdIn(userIds), TutorCenter::getUser);
        Map<Long, Client> clientMap =
                toMapByUserId(clientRepository.findByUser_UserIdIn(userIds), Client::getUser);

        return new ProfileMaps(adminMap, tutorMap, centerMap, clientMap);
    }

    private <T> Map<Long, T> toMapByUserId(List<T> items, Function<T, User> userExtractor) {
        return items.stream()
                .collect(Collectors.toMap(item -> userExtractor.apply(item).getUserId(), Function.identity()));
    }

    private record ProfileMaps(
            Map<Long, PlatformAdmin> admins,
            Map<Long, Tutor> tutors,
            Map<Long, TutorCenter> centers,
            Map<Long, Client> clients) {

        static ProfileMaps empty() {
            return new ProfileMaps(Map.of(), Map.of(), Map.of(), Map.of());
        }

        UserProfileBundle bundleFor(Long userId) {
            return UserProfileBundle.of(
                    admins.get(userId), tutors.get(userId), centers.get(userId), clients.get(userId));
        }
    }
}
