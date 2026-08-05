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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    private final SystemParameterRepository systemParameterRepository;
    private final AuditLogService auditLogService;

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

    @Override
    @Transactional
    public SystemParameterResponse createParameter(UpsertSystemParameterRequest request) {
        String paramKey = normalizeKey(request);
        systemParameterRepository.findByParamKey(paramKey).ifPresent(existing -> {
            throw new IllegalArgumentException("Khóa tham số đã tồn tại: " + paramKey);
        });

        SystemParameter parameter = new SystemParameter();
        parameter.setParamKey(paramKey);
        parameter.setParamValue(normalizeValue(request));
        parameter.setDescription(normalizeText(request.getDescription()));
        SystemParameter saved = systemParameterRepository.save(parameter);
        auditLogService.record("CREATE_SYSTEM_PARAMETER", "SystemParameter", saved.getParameterId(), null, request);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SystemParameterResponse updateParameter(Long parameterId, UpsertSystemParameterRequest request) {
        SystemParameter parameter = getRequiredParameter(parameterId);
        String paramKey = normalizeKey(request);
        systemParameterRepository.findByParamKey(paramKey).ifPresent(existing -> {
            if (!existing.getParameterId().equals(parameterId)) {
                throw new IllegalArgumentException("Khóa tham số đã tồn tại: " + paramKey);
            }
        });

        SystemParameterResponse oldValue = toResponse(parameter);
        parameter.setParamKey(paramKey);
        parameter.setParamValue(normalizeValue(request));
        parameter.setDescription(normalizeText(request.getDescription()));
        SystemParameter saved = systemParameterRepository.save(parameter);
        auditLogService.record("UPDATE_SYSTEM_PARAMETER", "SystemParameter", saved.getParameterId(), oldValue, request);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteParameter(Long parameterId) {
        SystemParameter parameter = getRequiredParameter(parameterId);
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
