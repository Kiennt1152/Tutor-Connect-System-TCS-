package com.tcs.module.messaging.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.messaging.dto.request.PreviewNotificationTemplateRequest;
import com.tcs.module.messaging.dto.request.UpsertNotificationTemplateRequest;
import com.tcs.module.messaging.dto.response.NotificationTemplatePreviewResponse;
import com.tcs.module.messaging.dto.response.NotificationTemplateResponse;
import com.tcs.module.messaging.entity.NotificationTemplate;
import com.tcs.module.messaging.repository.NotificationTemplateRepository;
import com.tcs.module.messaging.service.NotificationTemplateService;
import com.tcs.module.platform.service.AuditLogService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * [UC-35] HIỆN THỰC QUẢN TRỊ MẪU THÔNG BÁO & RENDER PLACEHOLDER (NOTIFICATION TEMPLATE SERVICE IMPL)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-07-29
 * 
 * Mô tả Use Case:
 *   - Dịch vụ hiện thực hóa việc quản trị mẫu thông báo động toàn sàn và nội suy biến số thời gian thực.
 *   - Tự động ghi vết kiểm toán Audit Log cho các thao tác cấu hình mẫu thông báo.
 * 
 * Chức năng chính:
 *   1. CRUD mẫu thông báo: Quản lý tiêu đề, nội dung, mã sự kiện và kênh gửi thông báo.
 *   2. Phân tích placeholder: Nhận diện biến số động định dạng {{variableName}} bằng biểu thức chính quy Regex.
 *   3. Render bản xem trước (Preview): Điền dữ liệu giả lập vào mẫu để kiểm tra hiển thị trước khi gửi thật.
 *   4. Ghi vết kiểm toán: Tự động ghi nhận lịch sử thay đổi vào AuditLogService đảm bảo an toàn vận hành.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tiếp nhận yêu cầu tạo/sửa template, bóc tách danh sách placeholders qua Pattern Regex.
 *   - Bước 2: Kiểm tra tính toàn vẹn cấu trúc và lưu vào bảng notification_templates.
 *   - Bước 3: Ghi vết thay đổi vào nhật ký kiểm toán AuditLogService.
 *   - Bước 4: Phục vụ render thông báo động cho các luồng gửi thông báo tự động toàn hệ thống.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9_.-]*)\\s*}}");
    private static final Pattern ANY_PLACEHOLDER = Pattern.compile("\\{\\{[^{}]*}}");

    private final NotificationTemplateRepository repository;
    private final AuditLogService auditLogService;

    /**
     * [UC-35] Lấy danh sách toàn bộ mẫu thông báo hệ thống được sắp xếp theo mã code.
     * 
     * @return Danh sách NotificationTemplateResponse
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> findAll() {
        return repository.findAll().stream()
                .sorted((left, right) -> left.getCode().compareToIgnoreCase(right.getCode()))
                .map(this::toResponse)
                .toList();
    }

    /**
     * [UC-35] Tìm kiếm thông tin chi tiết một mẫu thông báo theo ID.
     * 
     * @param templateId ID mẫu thông báo
     * @return NotificationTemplateResponse chi tiết mẫu
     */
    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse findById(Long templateId) {
        return toResponse(required(templateId));
    }

    /**
     * [UC-35] Tạo mới mẫu thông báo hệ thống và lưu trữ vào CSDL.
     * 
     * Luồng xử lý:
     * 1. Chuẩn hóa mã code và kiểm tra tính duy nhất.
     * 2. Xác thực tính hợp lệ của cú pháp placeholder trong tiêu đề và nội dung.
     * 3. Lưu bản ghi NotificationTemplate và ghi nhận vết kiểm toán CREATE_NOTIFICATION_TEMPLATE.
     * 
     * @param request Dữ liệu tạo mới mẫu
     * @return NotificationTemplateResponse thông tin mẫu đã tạo
     */
    @Override
    @Transactional
    public NotificationTemplateResponse create(UpsertNotificationTemplateRequest request) {
        String code = normalizeCode(request.getCode());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã template đã tồn tại.");
        }
        NotificationTemplate template = new NotificationTemplate();
        apply(template, request, code);
        NotificationTemplate saved = repository.save(template);
        auditLogService.record("CREATE_NOTIFICATION_TEMPLATE", "NotificationTemplate", saved.getTemplateId(), null,
                toResponse(saved));
        return toResponse(saved);
    }

    /**
     * [UC-35] Cập nhật thông tin chi tiết của mẫu thông báo hệ thống.
     * 
     * Luồng xử lý:
     * 1. Tìm bản ghi theo ID, kiểm tra mã code không bị trùng lặp với mẫu khác.
     * 2. Xác thực cú pháp placeholder và áp dụng các thuộc tính mới.
     * 3. Lưu cập nhật và ghi nhận nhật ký kiểm toán UPDATE_NOTIFICATION_TEMPLATE.
     * 
     * @param templateId ID mẫu cần sửa
     * @param request Dữ liệu cập nhật
     * @return NotificationTemplateResponse thông tin mẫu sau khi cập nhật
     */
    @Override
    @Transactional
    public NotificationTemplateResponse update(Long templateId, UpsertNotificationTemplateRequest request) {
        NotificationTemplate template = required(templateId);
        NotificationTemplateResponse oldValue = toResponse(template);
        String code = normalizeCode(request.getCode());
        repository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getTemplateId().equals(templateId))
                .ifPresent(existing -> { throw new IllegalArgumentException("Mã template đã tồn tại."); });
        apply(template, request, code);
        NotificationTemplate saved = repository.save(template);
        auditLogService.record("UPDATE_NOTIFICATION_TEMPLATE", "NotificationTemplate", templateId, oldValue,
                toResponse(saved));
        return toResponse(saved);
    }

    /**
     * [UC-35] Vô hiệu hóa mẫu thông báo hệ thống.
     * 
     * Luồng xử lý:
     * 1. Tìm mẫu thông báo và kiểm tra nếu đang bật.
     * 2. Đặt enabled = false và lưu vào CSDL.
     * 3. Ghi nhận nhật ký kiểm toán DISABLE_NOTIFICATION_TEMPLATE.
     * 
     * @param templateId ID mẫu cần tắt
     * @return NotificationTemplateResponse thông tin mẫu sau khi tắt
     */
    @Override
    @Transactional
    public NotificationTemplateResponse disable(Long templateId) {
        NotificationTemplate template = required(templateId);
        NotificationTemplateResponse oldValue = toResponse(template);
        if (!Boolean.TRUE.equals(template.getEnabled())) {
            throw new IllegalArgumentException("Template đã được tắt.");
        }
        template.setEnabled(false);
        NotificationTemplate saved = repository.save(template);
        auditLogService.record("DISABLE_NOTIFICATION_TEMPLATE", "NotificationTemplate", templateId, oldValue,
                toResponse(saved));
        return toResponse(saved);
    }

    @Override
    /**
     * [UC-35] Xem trước kết quả nội suy dữ liệu vào mẫu thông báo.
     * 
     * Luồng xử lý:
     * 1. Xác thực cú pháp các chuỗi mẫu tiêu đề và nội dung.
     * 2. Thực hiện thay thế các biến số placeholder bằng giá trị tương ứng.
     * 3. Phát hiện và liệt kê các placeholder còn thiếu giá trị.
     * 
     * @param request Yêu cầu xem trước
     * @return NotificationTemplatePreviewResponse kết quả xem trước
     */
    @Transactional(readOnly = true)
    public NotificationTemplatePreviewResponse preview(PreviewNotificationTemplateRequest request) {
        validateSyntax(request.getTitleTemplate());
        validateSyntax(request.getContentTemplate());
        Map<String, ?> variables = request.getVariables() == null ? Map.of() : request.getVariables();
        String title = render(request.getTitleTemplate(), variables);
        String content = render(request.getContentTemplate(), variables);
        Set<String> unresolved = placeholders(title + "\n" + content);
        return NotificationTemplatePreviewResponse.builder()
                .title(title)
                .content(content)
                .unresolvedPlaceholders(unresolved)
                .build();
    }

    /**
     * [UC-35] Nội suy và kết xuất nội dung từ mẫu thông báo đang có hiệu lực.
     * 
     * Luồng xử lý:
     * 1. Tìm kiếm mẫu theo code và kiểm tra cờ enabled.
     * 2. Nội suy các biến vào tiêu đề và nội dung nếu tìm thấy mẫu hoạt động.
     * 
     * @param code Mã mẫu thông báo
     * @param variables Bộ biến số thay thế
     * @return Optional chứa RenderedTemplate nếu có mẫu phù hợp, ngược lại empty
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<RenderedTemplate> renderEnabled(String code, Map<String, ?> variables) {
        if (code == null || code.isBlank()) return Optional.empty();
        return repository.findByCodeIgnoreCase(code.trim())
                .filter(template -> Boolean.TRUE.equals(template.getEnabled()))
                .map(template -> new RenderedTemplate(
                        render(template.getTitleTemplate(), variables == null ? Map.of() : variables),
                        render(template.getContentTemplate(), variables == null ? Map.of() : variables)));
    }

    private void apply(NotificationTemplate template, UpsertNotificationTemplateRequest request, String code) {
        String title = request.getTitleTemplate().trim();
        String content = request.getContentTemplate().trim();
        validateSyntax(title);
        validateSyntax(content);
        template.setCode(code);
        template.setTitleTemplate(title);
        template.setContentTemplate(content);
        template.setChannel(request.getChannel().trim().toUpperCase(Locale.ROOT));
        template.setDescription(normalizeNullable(request.getDescription()));
        template.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private void validateSyntax(String source) {
        String withoutValid = PLACEHOLDER.matcher(source).replaceAll("");
        if (ANY_PLACEHOLDER.matcher(withoutValid).find()) {
            throw new IllegalArgumentException("Placeholder không hợp lệ. Dùng cú pháp {{ten_bien}}.");
        }
    }

    private String render(String source, Map<String, ?> variables) {
        Matcher matcher = PLACEHOLDER.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? matcher.group() : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Set<String> placeholders(String source) {
        Matcher matcher = PLACEHOLDER.matcher(source);
        Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) result.add(matcher.group(1));
        return result;
    }

    private NotificationTemplate required(Long templateId) {
        return repository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy notification template: " + templateId));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private NotificationTemplateResponse toResponse(NotificationTemplate template) {
        return NotificationTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .code(template.getCode())
                .titleTemplate(template.getTitleTemplate())
                .contentTemplate(template.getContentTemplate())
                .channel(template.getChannel())
                .description(template.getDescription())
                .enabled(Boolean.TRUE.equals(template.getEnabled()))
                .placeholders(placeholders(template.getTitleTemplate() + "\n" + template.getContentTemplate()))
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
