package com.tcs.module.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.response.AuditLogResponse;
import com.tcs.module.platform.dto.response.PageAuditLogResponse;
import com.tcs.module.platform.entity.AuditLog;
import com.tcs.module.platform.repository.AuditLogRepository;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.ClientRepository;

/**
 * ============================================================================
 * [UC-61] NHẬT KÝ KIỂM TOÁN HỆ THỐNG BẤT BIẾN (AUDIT LOG SERVICE IMPLEMENTATION)
 * ============================================================================
 * * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * * Mô tả Use Case:
 *   - Ghi nhận và lưu trữ nhật ký kiểm toán (Audit Trail) bất biến cho toàn bộ nền tảng.
 *   - Phục vụ việc thanh tra, truy vết trách nhiệm và đối soát các thao tác quản trị nhạy cảm.
 * * Chức năng chính:
 *   1. Ghi nhận hành vi quản trị: Lưu vết mọi thay đổi tài chính, phí nền tảng, phê duyệt KYC, xử lý khiếu nại, chế tài.
 *   2. Lưu vết trạng thái thay đổi: Ghi nhận giá trị cũ (oldValue) và mới (newValue) dưới định dạng JSON.
 *   3. Ghi vết ngữ cảnh mạng: Tự động trích xuất IP client, User-Agent từ `HttpServletRequest` qua Spring RequestContext.
 *   4. Đảm bảo tính bất biến (Immutable): Chỉ hỗ trợ ghi mới (INSERT) và đọc (SELECT), tuyệt đối không sửa hoặc xóa.
 *   5. Truy vấn và ánh xạ dữ liệu: Hỗ trợ phân trang, lọc theo module, hành động và làm giàu thông tin người thực hiện.
 * * Luồng xử lý chính:
 *   - Bước 1: Service nghiệp vụ gọi hàm `logAction` hoặc `logActionWithPayload`.
 *   - Bước 2: Hệ thống phân giải `User` thực hiện và trích xuất IP/User-Agent từ context HTTP hiện hành.
 *   - Bước 3: Tạo bản ghi thực thể `AuditLog` và lưu vào CSDL thông qua `AuditLogRepository`.
 *   - Bước 4: Admin tra cứu nhật ký (`getAuditLogs`), service phân trang và làm giàu thông tin người dùng qua mapper.
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AuthHelper authHelper;
    private final ObjectMapper objectMapper;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;

    // =========================================================================
    // LUỒNG 11: GIÁM SÁT NHẬT KÝ KIỂM TOÁN & SO VẾT THAY ĐỔI JSON DIFF (UC-61)
    // =========================================================================

    /**
     * [UC-61] Ghi nhận vết kiểm toán tự động phân giải định danh người dùng từ SecurityContext.
     * 
     * Luồng xử lý:
     * 1. Phân giải userId của người thực hiện thông qua AuthHelper.
     * 2. Bắt ngoại lệ an toàn nếu request chạy ngầm hoặc không có token JWT.
     * 3. Chuyển giao sang phương thức record có tham số actorUserId.
     * 
     * @param action Mã hành động
     * @param entityType Loại đối tượng bị tác động
     * @param entityId ID đối tượng
     * @param oldValue Dữ liệu trước thay đổi
     * @param newValue Dữ liệu sau thay đổi
     */
    // Luồng 11 - Bước 1: Ghi nhận vết kiểm toán tự động từ SecurityContext
    @Override
    public void record(String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        Long userId = null;
        try {
            userId = authHelper.currentUserId();
        } catch (Exception e) {
            log.warn("Could not retrieve current user for audit log", e);
        }
        record(userId, action, entityType, entityId, oldValue, newValue);
    }

    /**
     * [UC-61] Khởi tạo thực thể AuditLog, tuần tự hóa JSON Diff và trích xuất địa chỉ IP/User-Agent.
     * 
     * Luồng xử lý:
     * 1. Khởi tạo đối tượng AuditLog với các thông tin nghiệp vụ cơ bản và thời gian hiện tại.
     * 2. Tìm kiếm và liên kết thông tin thực thể User của người thực hiện nếu có actorUserId.
     * 3. Chuyển đổi đối tượng oldValue và newValue thành chuỗi JSON đại diện cho ảnh chụp thay đổi.
     * 4. Trích xuất địa chỉ IP và trình duyệt người dùng từ ServletRequestAttributes hiện hành.
     * 5. Lưu bản ghi bất biến vào cơ sở dữ liệu qua AuditLogRepository.
     * 
     * @param actorUserId ID người thực hiện hành động
     * @param action Tên hành động
     * @param entityType Loại thực thể
     * @param entityId ID thực thể
     * @param oldValue Đối tượng cũ trước thay đổi
     * @param newValue Đối tượng mới sau thay đổi
     */
    // Luồng 11 - Bước 2: Khởi tạo thực thể AuditLog, tuần tự hóa JSON Diff và trích xuất IP/User-Agent
    @Override
    public void record(Long actorUserId, String action, String entityType, Long entityId, Object oldValue, Object newValue) {
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setCreatedAt(LocalDateTime.now());

        try {
            if (actorUserId != null) {
                userRepository.findById(actorUserId).ifPresent(auditLog::setActor);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve actor for audit log", e);
        }

        // Tuần tự hóa đối tượng cũ và mới sang chuỗi JSON (JSON Diff snapshot)
        try {
            if (oldValue != null) {
                auditLog.setOldValue(objectMapper.writeValueAsString(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValue(objectMapper.writeValueAsString(newValue));
            }
        } catch (Exception e) {
            log.warn("Could not serialize audit log values", e);
        }

        // Tự động bắt địa chỉ IP và định danh trình duyệt Client thông qua RequestContextHolder
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.warn("Could not retrieve request attributes for audit log", e);
        }

        auditLogRepository.save(auditLog);
    }

    /**
     * [UC-61] Tra cứu và phân trang danh sách nhật ký kiểm toán đa tiêu chí phục vụ thanh tra hệ thống.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa từ khóa tìm kiếm và vai trò người dùng (loại bỏ khoảng trắng, đổi hoa/thường).
     * 2. Thực thi truy vấn phân trang qua AuditLogRepository.search() với các tiêu chí lọc kết hợp.
     * 3. Chuyển đổi danh sách thực thể AuditLog sang DTO AuditLogResponse, làm giàu thông tin vai trò người thực hiện.
     * 4. Đóng gói kết quả phân trang PageAuditLogResponse trả về cho giao diện quản trị.
     * 
     * @param actorId ID người thực hiện
     * @param actorRole Vai trò người thực hiện
     * @param action Tên hành động
     * @param entityType Loại thực thể
     * @param keyword Từ khóa tìm kiếm
     * @param from Mốc thời gian bắt đầu
     * @param to Mốc thời gian kết thúc
     * @param page Số trang (bắt đầu từ 0)
     * @param size Số phần tử trên mỗi trang
     * @return PageAuditLogResponse kết quả tra cứu nhật ký phân trang
     */
    // Luồng 11 - Bước 3: Tra cứu & phân trang danh sách nhật ký kiểm toán đa tiêu chí
    @Override
    public PageAuditLogResponse search(Long actorId, String actorRole, String action, String entityType,
            String keyword, LocalDateTime from, LocalDateTime to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        String normalizedRole = (actorRole == null || actorRole.isBlank()) ? null : actorRole.trim().toUpperCase();
        Page<AuditLog> auditLogs = auditLogRepository.search(
                actorId, action, entityType, normalizedKeyword, normalizedRole, from, to, pageable);

        List<AuditLogResponse> content = auditLogs.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageAuditLogResponse.builder()
                .content(content)
                .page(auditLogs.getNumber())
                .size(auditLogs.getSize())
                .totalElements(auditLogs.getTotalElements())
                .totalPages(auditLogs.getTotalPages())
                .build();
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        String actorRole = null;
        if (actor != null) {
            UserProfileBundle profiles = loadProfiles(actor.getUserId());
            actorRole = platformMapper.resolveRole(profiles).name();
        }

        return AuditLogResponse.builder()
                .auditId(auditLog.getAuditId())
                .actorId(actor != null ? actor.getUserId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .actorRole(actorRole)
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .oldValue(auditLog.getOldValue())
                .newValue(auditLog.getNewValue())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null)
        );
    }
}
