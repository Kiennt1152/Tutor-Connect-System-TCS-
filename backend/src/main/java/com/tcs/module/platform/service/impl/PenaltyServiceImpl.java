package com.tcs.module.platform.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.finance.repository.DisputeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.platform.dto.request.IssuePenaltyRequest;
import com.tcs.module.platform.dto.request.RevokePenaltyRequest;
import com.tcs.module.platform.dto.response.PagePenaltyResponse;
import com.tcs.module.platform.dto.response.PenaltyResponse;
import com.tcs.module.platform.entity.UserPenalty;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.platform.repository.CircumventionEventRepository;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.platform.repository.SupportTicketRepository;
import com.tcs.module.platform.repository.UserPenaltyRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.platform.service.PenaltyService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * DỊCH VỤ QUẢN LÝ VÀ THỰC THI CHẾ TÀI XỬ PHẠT (PENALTY SERVICE IMPLEMENTATION)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả nghiệp vụ:
 *   - Quản trị viên ban hành các hình phạt (Cấm tạm thời, Cấm vĩnh viễn, Cảnh cáo, Hạn chế tính năng).
 *   - Tự động thay đổi trạng thái tài khoản người dùng (ACTIVE <-> BANNED) tương ứng với án phạt.
 *   - Tác vụ định kỳ Scheduled tự động quét và mãn hạn các án phạt cấm tạm thời, khôi phục tài khoản người dùng.
 *   - Kiểm tra ràng buộc hợp lệ: không tự phạt bản thân, không phạt Admin khác, kiểm tra nguồn xử lý (Dispute, Ticket, Report, Circumvention).
 *   - Ghi vết Audit Log và gửi thông báo Notification tự động tới người dùng bị phạt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final UserPenaltyRepository userPenaltyRepository;
    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final ReportRepository reportRepository;
    private final CircumventionEventRepository circumventionEventRepository;
    private final DisputeRepository disputeRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;
    private final NotificationDispatchService notificationDispatchService;

    /** Tập hợp các loại án phạt dẫn đến việc khóa tài khoản người dùng */
    private static final Set<UserPenaltyType> BAN_TYPES = Set.of(
            UserPenaltyType.TEMPORARY_BAN, UserPenaltyType.PERMANENT_BAN);

    /** Tập hợp các nguồn gốc tạo án phạt hợp lệ từ các phân hệ khác nhau */
    private static final Set<String> ALLOWED_SOURCE_TYPES = Set.of(
            "REPORT", "CIRCUMVENTION", "DISPUTE", "TICKET", "DIRECT");

    /**
     * Lấy thông tin tài khoản Quản trị viên (PlatformAdmin) hiện tại đang thực hiện thao tác.
     * 
     * @return đối tượng PlatformAdmin của phiên làm việc hiện tại
     * @throws ResourceNotFoundException nếu không tìm thấy hồ sơ quản trị viên
     */
    private PlatformAdmin currentAdminOrThrow() {
        Long adminUserId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        return platformAdminRepository.findByUser_UserId(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
    }

    // =========================================================================
    // LUỒNG 13: BAN HÀNH QUYẾT ĐỊNH XỬ PHẠT & TỰ ĐỘNG MÃN HẠN PHẠT (UC-60)
    // =========================================================================

    // Luồng 13 - Tác vụ chạy ngầm định kỳ 5 phút/lần tự động mở khóa khi hết hạn phạt cấm tạm thời
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void expireOverduePenalties() {
        LocalDateTime now = LocalDateTime.now();
        // Lấy danh sách các án phạt đang ACTIVE nhưng đã quá hạn expiresAt < now
        List<UserPenalty> overdue = userPenaltyRepository.findByStatusAndExpiresAtBefore(UserPenaltyStatus.ACTIVE, now);
        if (overdue.isEmpty()) {
            return;
        }

        for (UserPenalty penalty : overdue) {
            penalty.setStatus(UserPenaltyStatus.EXPIRED);
            userPenaltyRepository.save(penalty);

            User user = penalty.getUser();
            // Nếu người dùng không còn án phạt cấm nào khác đang hiệu lực -> Khôi phục về ACTIVE
            if (!hasActiveBan(user.getUserId())) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.save(user);
            }
        }
        log.info("Expired {} overdue user penalties", overdue.size());
    }

    /**
     * Tìm kiếm và phân trang danh sách án phạt theo bộ lọc đa chiều.
     * 
     * @param userId     ID người dùng cần lọc (hoặc null nếu xem tất cả)
     * @param status     trạng thái án phạt (ACTIVE, EXPIRED, REVOKED)
     * @param type       loại án phạt (WARNING, TEMPORARY_BAN, PERMANENT_BAN, FEATURE_RESTRICTION)
     * @param sourceType nguồn gốc phát sinh án phạt (REPORT, DISPUTE, TICKET,...)
     * @param page       chỉ số trang (0-indexed)
     * @param size       số lượng phần tử trên mỗi trang
     * @return danh sách án phạt đã được phân trang và đóng gói DTO
     */
    @Override
    @Transactional
    public PagePenaltyResponse listPenalties(Long userId, UserPenaltyStatus status, UserPenaltyType type, String sourceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSourceType = (sourceType != null && !sourceType.isBlank()) ? sourceType.trim().toUpperCase(Locale.ROOT) : null;
        Page<UserPenalty> penaltyPage = userPenaltyRepository.search(userId, status, type, normalizedSourceType, pageable);
        
        List<PenaltyResponse> content = penaltyPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagePenaltyResponse.builder()
                .content(content)
                .page(penaltyPage.getNumber())
                .size(penaltyPage.getSize())
                .totalElements(penaltyPage.getTotalElements())
                .totalPages(penaltyPage.getTotalPages())
                .build();
    }

    /**
     * Ban hành một quyết định xử phạt người dùng vi phạm quy chế nền tảng.
     * 
     * @param request thông tin chi tiết về án phạt (userId, loại phạt, lý do, bằng chứng, thời hạn)
     * @return thông tin án phạt vừa được khởi tạo và lưu trữ
     */
    // Luồng 13 - Bước 4: Admin ban hành quyết định xử phạt vi phạm
    @Override
    @Transactional
    public PenaltyResponse issuePenalty(IssuePenaltyRequest request) {
        PlatformAdmin issuingAdmin = currentAdminOrThrow();
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Ràng buộc bảo vệ: Admin không thể tự phạt mình hoặc phạt Admin khác
        if (issuingAdmin.getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("Quản trị viên không thể tự áp dụng hình phạt.");
        }
        if (platformAdminRepository.findByUser_UserId(user.getUserId()).isPresent()) {
            throw new IllegalArgumentException("Không thể áp dụng hình phạt cho tài khoản quản trị viên khác.");
        }

        // Ràng buộc giải trình: Bắt buộc lý do xử phạt tối thiểu 20 ký tự
        if (request.getReason() == null || request.getReason().trim().length() < 20) {
            throw new IllegalArgumentException("Lý do xử phạt phải có ít nhất 20 ký tự.");
        }

        UserPenaltyType penaltyType;
        try {
            penaltyType = UserPenaltyType.valueOf(request.getPenaltyType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loại hình phạt không hợp lệ: " + request.getPenaltyType());
        }
        
        if (penaltyType == UserPenaltyType.TEMPORARY_BAN) {
            if (request.getExpiresAt() == null || request.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Thời gian hết hạn không hợp lệ cho cấm tạm thời");
            }
        }
        
        if (penaltyType == UserPenaltyType.PERMANENT_BAN) {
            request.setExpiresAt(null);
        }
        if (penaltyType == UserPenaltyType.FEATURE_RESTRICTION) {
            validateRestrictionCodes(request.getRestrictionDetails());
        }

        String rawSourceType = request.getSourceType();
        String sourceType = (rawSourceType != null && !rawSourceType.isBlank()) 
                ? rawSourceType.trim().toUpperCase(Locale.ROOT) 
                : null;
        Long sourceId = request.getSourceId();

        if (sourceType != null) {
            if (!ALLOWED_SOURCE_TYPES.contains(sourceType)) {
                throw new IllegalArgumentException("Loại nguồn xử phạt không hợp lệ: " + sourceType + ". Chỉ chấp nhận: " + ALLOWED_SOURCE_TYPES);
            }
            if (!sourceType.equals("DIRECT") && sourceId == null) {
                throw new IllegalArgumentException("sourceId là bắt buộc khi sourceType là " + sourceType);
            }
            if (sourceId != null) {
                validateSourceExists(sourceType, sourceId);
            }
        } else if (sourceId != null) {
            throw new IllegalArgumentException("sourceType là bắt buộc khi có sourceId.");
        }

        String rawTaskId = request.getSourceTaskId();
        String sourceTaskId = (rawTaskId != null && !rawTaskId.isBlank())
                ? rawTaskId.trim()
                : (sourceType != null && sourceId != null ? sourceType + "-" + sourceId : null);

        request.setSourceType(sourceType);
        request.setSourceId(sourceId);
        request.setSourceTaskId(sourceTaskId);

        // Khởi tạo và lưu quyết định xử phạt UserPenalty
        UserPenalty penalty = new UserPenalty();
        penalty.setUser(user);
        penalty.setIssuedBy(issuingAdmin);
        penalty.setPenaltyType(penaltyType);
        penalty.setReason(request.getReason());
        penalty.setEvidenceUrls(request.getEvidenceUrls());
        penalty.setRestrictionDetails(request.getRestrictionDetails());
        penalty.setStartsAt(LocalDateTime.now());
        penalty.setExpiresAt(request.getExpiresAt());
        penalty.setStatus(UserPenaltyStatus.ACTIVE);
        penalty.setCreatedAt(LocalDateTime.now());
        penalty.setSourceType(sourceType);
        penalty.setSourceId(sourceId);
        penalty.setSourceTaskId(sourceTaskId);

        // Cập nhật trạng thái tài khoản người dùng sang BANNED nếu bị cấm
        if (penaltyType == UserPenaltyType.TEMPORARY_BAN || penaltyType == UserPenaltyType.PERMANENT_BAN) {
            user.setStatus(UserStatus.BANNED);
            userRepository.save(user);
        }

        userPenaltyRepository.save(penalty);
        // Ghi Audit Log hành động ISSUE_PENALTY
        auditLogService.record("ISSUE_PENALTY", "UserPenalty", penalty.getPenaltyId(), null, request);
        // Phát thông báo khẩn tới người dùng vi phạm
        notificationDispatchService.notifyUserFromTemplate(
                user, NotificationType.SYSTEM, "PENALTY_ISSUED",
                Map.of(
                        "penaltyType", penaltyType.name(),
                        "reason", request.getReason(),
                        "expiresAt", request.getExpiresAt() == null ? "Không thời hạn" : request.getExpiresAt()),
                "Tài khoản của bạn vừa nhận một hình phạt",
                "Loại: " + penaltyType.name() + ". Lý do: " + request.getReason(),
                "PENALTY", penalty.getPenaltyId());
        return toResponse(penalty);
    }

    /**
     * Thu hồi/hủy bỏ một quyết định xử phạt đang có hiệu lực và khôi phục tài khoản nếu đủ điều kiện.
     * 
     * @param penaltyId ID án phạt cần thu hồi
     * @param request   lý do thu hồi án phạt từ Admin
     * @return thông tin án phạt sau khi thu hồi
     */
    @Override
    @Transactional
    public PenaltyResponse revokePenalty(Long penaltyId, RevokePenaltyRequest request) {
        PlatformAdmin revokingAdmin = currentAdminOrThrow();
        UserPenalty penalty = userPenaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình phạt"));

        if (penalty.getUser().getUserId().equals(revokingAdmin.getUser().getUserId())) {
            throw new IllegalArgumentException("Quản trị viên không thể tự thu hồi hình phạt của chính mình.");
        }

        if (penalty.getStatus() != UserPenaltyStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể thu hồi hình phạt đang hoạt động");
        }

        // Cập nhật trạng thái án phạt sang REVOKED
        penalty.setStatus(UserPenaltyStatus.REVOKED);
        penalty.setRevokedAt(LocalDateTime.now());
        penalty.setRevokedReason(request.getRevokedReason());
        userPenaltyRepository.save(penalty);
        auditLogService.record("REVOKE_PENALTY", "UserPenalty", penalty.getPenaltyId(), null, request);

        // Khôi phục trạng thái người dùng về ACTIVE nếu không còn án cấm nào khác
        User user = penalty.getUser();
        if (!hasActiveBan(user.getUserId())) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        return toResponse(penalty);
    }

    private void validateSourceExists(String sourceType, Long sourceId) {
        switch (sourceType.toUpperCase(Locale.ROOT)) {
            case "REPORT" -> {
                if (!reportRepository.existsById(sourceId)) {
                    throw new ResourceNotFoundException("Không tìm thấy báo cáo #" + sourceId);
                }
            }
            case "CIRCUMVENTION" -> {
                if (!circumventionEventRepository.existsById(sourceId) && !reportRepository.existsById(sourceId)) {
                    throw new ResourceNotFoundException("Không tìm thấy sự kiện lách sàn hoặc báo cáo #" + sourceId);
                }
            }
            case "DISPUTE" -> {
                if (!disputeRepository.existsById(sourceId)) {
                    throw new ResourceNotFoundException("Không tìm thấy tranh chấp #" + sourceId);
                }
            }
            case "TICKET" -> {
                if (!supportTicketRepository.existsById(sourceId)) {
                    throw new ResourceNotFoundException("Không tìm thấy ticket #" + sourceId);
                }
            }
            case "DIRECT" -> {
                // Direct penalty, no specific entity existence required
            }
            default -> throw new IllegalArgumentException("Nguồn xử lý không được hỗ trợ: " + sourceType);
        }
    }

    private boolean hasActiveBan(Long userId) {
        return userPenaltyRepository.existsByUser_UserIdAndStatusAndPenaltyTypeIn(
                userId, UserPenaltyStatus.ACTIVE, BAN_TYPES);
    }

    private void validateRestrictionCodes(String details) {
        if (details == null || details.isBlank()) {
            throw new IllegalArgumentException("Hạn chế tính năng phải có mã tính năng.");
        }
        Set<String> allowed = Set.of("MESSAGING", "CLASS_POSTING", "CLASS_APPLICATION", "WITHDRAWAL");
        String normalized = details.toUpperCase(Locale.ROOT).trim();
        if (!(normalized.startsWith("{") || normalized.startsWith("["))
                || allowed.stream().noneMatch(normalized::contains)) {
            throw new IllegalArgumentException("restrictionDetails phải là JSON chứa mã tính năng hợp lệ: " + allowed);
        }
    }

    private PenaltyResponse toResponse(UserPenalty penalty) {
        User user = penalty.getUser();
        String displayName = user.getEmail(); // Fallback to email as requested
        
        PlatformAdmin admin = penalty.getIssuedBy();
        String adminName = admin != null && admin.getFullName() != null ? admin.getFullName() : (admin != null && admin.getUser() != null ? admin.getUser().getEmail() : "Unknown");
        
        return PenaltyResponse.builder()
                .penaltyId(penalty.getPenaltyId())
                .userId(user.getUserId())
                .userEmail(user.getEmail())
                .userName(displayName)
                .penaltyType(penalty.getPenaltyType().name())
                .reason(penalty.getReason())
                .evidenceUrls(penalty.getEvidenceUrls())
                .restrictionDetails(penalty.getRestrictionDetails())
                .startsAt(penalty.getStartsAt())
                .expiresAt(penalty.getExpiresAt())
                .status(penalty.getStatus().name())
                .revokedAt(penalty.getRevokedAt())
                .revokedReason(penalty.getRevokedReason())
                .createdAt(penalty.getCreatedAt())
                .issuedByName(adminName)
                .sourceType(penalty.getSourceType())
                .sourceId(penalty.getSourceId())
                .sourceTaskId(penalty.getSourceTaskId())
                .build();
    }
}
