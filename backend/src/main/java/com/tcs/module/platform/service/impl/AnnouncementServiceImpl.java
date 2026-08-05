package com.tcs.module.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.platform.dto.request.UpsertAnnouncementRequest;
import com.tcs.module.platform.dto.response.AnnouncementResponse;
import com.tcs.module.platform.service.AnnouncementService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final String PARAM_KEY = "SYSTEM_ANNOUNCEMENTS";

    private final SystemParameterRepository systemParameterRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final AuthHelper authHelper;
    private final AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnouncementItem {
        private Long announcementId;
        private String title;
        private String content;
        private UserRole targetRole;
        private Boolean active;
        private LocalDateTime startsAt;
        private LocalDateTime endsAt;
        private Long createdByAdminId;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements() {
        return loadItems().stream()
                .sorted(Comparator.comparing(AnnouncementItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncement(Long announcementId) {
        return toResponse(getRequired(announcementId, loadItems()));
    }

    @Override
    @Transactional
    public AnnouncementResponse createAnnouncement(UpsertAnnouncementRequest request) {
        List<AnnouncementItem> items = loadItems();
        AnnouncementItem item = new AnnouncementItem();
        item.setAnnouncementId(generateId(items));
        applyChanges(item, request);
        PlatformAdmin admin = currentAdminOrThrow();
        item.setCreatedByAdminId(admin.getAdminId());
        item.setCreatedByName(admin.getFullName());
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        items.add(item);
        saveItems(items);
        auditLogService.record("CREATE_ANNOUNCEMENT", "Announcement", item.getAnnouncementId(), null, request);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AnnouncementResponse updateAnnouncement(Long announcementId, UpsertAnnouncementRequest request) {
        List<AnnouncementItem> items = loadItems();
        AnnouncementItem item = getRequired(announcementId, items);
        applyChanges(item, request);
        item.setUpdatedAt(LocalDateTime.now());
        saveItems(items);
        auditLogService.record("UPDATE_ANNOUNCEMENT", "Announcement", announcementId, null, request);
        return toResponse(item);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long announcementId) {
        List<AnnouncementItem> items = loadItems();
        AnnouncementItem item = getRequired(announcementId, items);
        items.remove(item);
        saveItems(items);
        auditLogService.record("DELETE_ANNOUNCEMENT", "Announcement", announcementId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getVisibleAnnouncements(UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        return loadItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getActive()))
                .filter(i -> i.getStartsAt() == null || !now.isBefore(i.getStartsAt()))
                .filter(i -> i.getEndsAt() == null || !now.isAfter(i.getEndsAt()))
                .filter(i -> i.getTargetRole() == null || role == null || i.getTargetRole() == role)
                .sorted(Comparator.comparing(AnnouncementItem::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private List<AnnouncementItem> loadItems() {
        Optional<SystemParameter> paramOpt = systemParameterRepository.findByParamKey(PARAM_KEY);
        if (paramOpt.isEmpty() || paramOpt.get().getParamValue() == null || paramOpt.get().getParamValue().trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(paramOpt.get().getParamValue(), new TypeReference<List<AnnouncementItem>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveItems(List<AnnouncementItem> items) {
        try {
            String json = objectMapper.writeValueAsString(items);
            SystemParameter param = systemParameterRepository.findByParamKey(PARAM_KEY)
                    .orElseGet(() -> {
                        SystemParameter p = new SystemParameter();
                        p.setParamKey(PARAM_KEY);
                        p.setDescription("Danh sách thông báo hệ thống (JSON Array)");
                        return p;
                    });
            param.setParamValue(json);
            systemParameterRepository.save(param);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu cấu hình thông báo hệ thống: " + e.getMessage(), e);
        }
    }

    private Long generateId(List<AnnouncementItem> items) {
        long maxId = items.stream()
                .mapToLong(i -> i.getAnnouncementId() != null ? i.getAnnouncementId() : 0L)
                .max()
                .orElse(0L);
        return maxId + 1;
    }

    private AnnouncementItem getRequired(Long announcementId, List<AnnouncementItem> items) {
        return items.stream()
                .filter(i -> Objects.equals(i.getAnnouncementId(), announcementId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo: " + announcementId));
    }

    private PlatformAdmin currentAdminOrThrow() {
        Long adminUserId = authHelper.requireRole(UserRole.PLATFORM_ADMIN).getUserId();
        return platformAdminRepository.findByUser_UserId(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ quản trị viên"));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyChanges(AnnouncementItem item, UpsertAnnouncementRequest request) {
        String title = normalizeText(request.getTitle());
        if (title == null) {
            throw new IllegalArgumentException("Tiêu đề là bắt buộc.");
        }
        String content = normalizeText(request.getContent());
        if (content == null) {
            throw new IllegalArgumentException("Nội dung là bắt buộc.");
        }
        if (request.getStartsAt() != null && request.getEndsAt() != null
                && request.getEndsAt().isBefore(request.getStartsAt())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        item.setTitle(title);
        item.setContent(content);
        item.setTargetRole(request.getTargetRole());
        item.setActive(request.getActive() == null || request.getActive());
        item.setStartsAt(request.getStartsAt());
        item.setEndsAt(request.getEndsAt());
    }

    private AnnouncementResponse toResponse(AnnouncementItem item) {
        return AnnouncementResponse.builder()
                .announcementId(item.getAnnouncementId())
                .title(item.getTitle())
                .content(item.getContent())
                .targetRole(item.getTargetRole())
                .active(Boolean.TRUE.equals(item.getActive()))
                .startsAt(item.getStartsAt())
                .endsAt(item.getEndsAt())
                .createdByName(item.getCreatedByName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
