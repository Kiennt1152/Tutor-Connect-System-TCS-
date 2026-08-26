package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.service.SystemParameterService;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    // =========================================================================
    // LUỒNG 10: CẤU HÌNH THAM SỐ NỀN TẢNG & TỶ LỆ PHÍ ĐỘNG (UC-46)
    // =========================================================================

    // Danh sách các khóa cấu hình bắt buộc của hệ thống - Tuyệt đối không cho phép xóa hoặc đổi tên
    private static final Set<String> MANDATORY_KEYS = Set.of("PLATFORM_FEE_RATE", "ESCROW_HOLD_DAYS");

    private final SystemParameterRepository systemParameterRepository;
    private final AuditLogService auditLogService;

    // Luồng 10 - Bước 1: Tra cứu danh sách tham số hệ thống
    @Override
    @Transactional(readOnly = true)
    public List<SystemParameterResponse> getParameters(String prefix, String keyword) {
        String trimmedPrefix = normalizeText(prefix);
        String trimmedKeyword = normalizeText(keyword);
        String loweredKeyword = trimmedKeyword != null ? trimmedKeyword.toLowerCase(Locale.ROOT) : null;

        List<SystemParameter> source = trimmedPrefix != null
                ? systemParameterRepository.findByParamKeyStartingWith(trimmedPrefix)
                : systemParameterRepository.findAll();

        return source.stream()
                .filter(param -> loweredKeyword == null
                        || param.getParamKey().toLowerCase(Locale.ROOT).contains(loweredKeyword)
                        || (param.getParamValue() != null
                                && param.getParamValue().toLowerCase(Locale.ROOT).contains(loweredKeyword)))
                .sorted(Comparator.comparing(SystemParameter::getParamKey))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemParameterResponse getParameter(Long parameterId) {
        return toResponse(getRequiredParameter(parameterId));
    }

    // Luồng 10 - Bước 2: Tạo tham số mới kèm kiểm tra tính duy nhất của paramKey
    @Override
    @Transactional
    public SystemParameterResponse createParameter(UpsertSystemParameterRequest request) {
        String paramKey = normalizeKey(request);
        systemParameterRepository.findByParamKey(paramKey).ifPresent(existing -> {
            throw new IllegalArgumentException("Khóa tham số đã tồn tại: " + paramKey);
        });

        SystemParameter parameter = new SystemParameter();
        parameter.setParamKey(paramKey);
        parameter.setParamValue(validateValue(paramKey, normalizeValue(request)));
        parameter.setDescription(normalizeText(request.getDescription()));
        SystemParameter saved = systemParameterRepository.save(parameter);
        auditLogService.record("CREATE_SYSTEM_PARAMETER", "SystemParameter", saved.getParameterId(), null, request);
        return toResponse(saved);
    }

    // Luồng 10 - Bước 3: Cập nhật giá trị tham số (như PLATFORM_FEE_RATE: 0.10 -> 0.12)
    @Override
    @Transactional
    public SystemParameterResponse updateParameter(Long parameterId, UpsertSystemParameterRequest request) {
        SystemParameter parameter = getRequiredParameter(parameterId);
        String paramKey = normalizeKey(request);

        // Quy tắc an toàn: Không cho phép đổi tên khóa bắt buộc (PLATFORM_FEE_RATE, ESCROW_HOLD_DAYS)
        if (MANDATORY_KEYS.contains(parameter.getParamKey()) && !parameter.getParamKey().equals(paramKey)) {
            throw new IllegalArgumentException("Không thể đổi tên khóa cấu hình bắt buộc: " + parameter.getParamKey());
        }
        systemParameterRepository.findByParamKey(paramKey).ifPresent(existing -> {
            if (!existing.getParameterId().equals(parameterId)) {
                throw new IllegalArgumentException("Khóa tham số đã tồn tại: " + paramKey);
            }
        });

        SystemParameterResponse oldValue = toResponse(parameter);
        parameter.setParamKey(paramKey);
        // Xác thực giá trị hợp lệ theo từng loại tham số
        parameter.setParamValue(validateValue(paramKey, normalizeValue(request)));
        parameter.setDescription(normalizeText(request.getDescription()));
        SystemParameter saved = systemParameterRepository.save(parameter);

        // Ghi nhật ký Audit Log so vết thay đổi JSON Diff
        auditLogService.record("UPDATE_SYSTEM_PARAMETER", "SystemParameter", saved.getParameterId(), oldValue, request);
        return toResponse(saved);
    }

    // Luồng 10 - Bước 4: Xóa tham số tùy chỉnh (Chặn tuyệt đối không cho xóa MANDATORY_KEYS)
    @Override
    @Transactional
    public void deleteParameter(Long parameterId) {
        SystemParameter parameter = getRequiredParameter(parameterId);
        if (MANDATORY_KEYS.contains(parameter.getParamKey())) {
            throw new IllegalArgumentException("Không thể xóa khóa cấu hình bắt buộc: " + parameter.getParamKey());
        }
        SystemParameterResponse oldValue = toResponse(parameter);
        systemParameterRepository.delete(parameter);
        auditLogService.record("DELETE_SYSTEM_PARAMETER", "SystemParameter", parameterId, oldValue, null);
    }

    private SystemParameter getRequiredParameter(Long parameterId) {
        return systemParameterRepository.findById(parameterId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tham số hệ thống: " + parameterId));
    }

    private String normalizeKey(UpsertSystemParameterRequest request) {
        String key = normalizeText(request.getParamKey());
        if (key == null) {
            throw new IllegalArgumentException("Khóa tham số là bắt buộc.");
        }
        return key;
    }

    private String normalizeValue(UpsertSystemParameterRequest request) {
        String value = request.getParamValue() != null ? request.getParamValue().trim() : null;
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Giá trị tham số là bắt buộc.");
        }
        return value;
    }

    // Luồng 10 - Thuật toán kiểm tra giới hạn biên số học (Boundary Check)
    private String validateValue(String key, String value) {
        try {
            // Tỷ lệ phí sàn bắt buộc từ 0.00 đến 0.50 (0% đến 50%)
            if ("PLATFORM_FEE_RATE".equals(key)) {
                BigDecimal rate = new BigDecimal(value);
                if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("0.50")) > 0) {
                    throw new IllegalArgumentException("PLATFORM_FEE_RATE phải từ 0.00 đến 0.50.");
                }
                return rate.stripTrailingZeros().toPlainString();
            }
            // Thời gian giữ tiền cọc Escrow từ 1 đến 365 ngày
            if ("ESCROW_HOLD_DAYS".equals(key)) {
                int days = Integer.parseInt(value);
                if (days < 1 || days > 365) {
                    throw new IllegalArgumentException("ESCROW_HOLD_DAYS phải từ 1 đến 365.");
                }
                return String.valueOf(days);
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Giá trị không đúng định dạng cho " + key + ".");
        }
        return value;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SystemParameterResponse toResponse(SystemParameter parameter) {
        return SystemParameterResponse.builder()
                .parameterId(parameter.getParameterId())
                .paramKey(parameter.getParamKey())
                .paramValue(parameter.getParamValue())
                .description(parameter.getDescription())
                .build();
    }
}
