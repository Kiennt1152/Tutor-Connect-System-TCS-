package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.marketplace.dto.SubstitutionEntry;
import com.tcs.module.marketplace.service.SubstitutionService;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubstitutionServiceImpl implements SubstitutionService {

    private static final String ASSIST_PREFIX = "assist:"; // assist:{classId} -> tutorId
    private static final String SUB_PREFIX = "substi:"; // substi:{classId}:{date} -> JSON
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemParameterRepository systemParameterRepository;

    /** Cấu trúc JSON lưu trong param_value của một yêu cầu dạy thay. */
    private static final class Payload {
        public Long tutorId;
        public String status;
        public String reason;
    }

    // ===================== Gia sư phụ =====================

    private String assistKey(Long classId) {
        return ASSIST_PREFIX + classId;
    }

    @Override
    @Transactional
    public void assignAssistant(Long classId, Long tutorId) {
        String key = assistKey(classId);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        param.setParamValue(String.valueOf(tutorId));
        param.setDescription("Gia sư phụ của lớp");
        systemParameterRepository.save(param);
    }

    @Override
    @Transactional
    public void removeAssistant(Long classId) {
        systemParameterRepository.findByParamKey(assistKey(classId))
                .ifPresent(systemParameterRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findAssistant(Long classId) {
        return systemParameterRepository.findByParamKey(assistKey(classId))
                .map(p -> parseLong(p.getParamValue()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> findAssistants(Collection<Long> classIds) {
        Set<Long> ids = new HashSet<>(classIds);
        Map<Long, Long> result = new HashMap<>();
        for (SystemParameter p : systemParameterRepository.findByParamKeyStartingWith(ASSIST_PREFIX)) {
            Long classId = parseLong(p.getParamKey().substring(ASSIST_PREFIX.length()));
            if (classId != null && ids.contains(classId)) {
                result.put(classId, parseLong(p.getParamValue()));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findClassIdsByAssistant(Long tutorId) {
        List<Long> classIds = new java.util.ArrayList<>();
        for (SystemParameter p : systemParameterRepository.findByParamKeyStartingWith(ASSIST_PREFIX)) {
            if (tutorId.equals(parseLong(p.getParamValue()))) {
                Long classId = parseLong(p.getParamKey().substring(ASSIST_PREFIX.length()));
                if (classId != null) {
                    classIds.add(classId);
                }
            }
        }
        return classIds;
    }

    // ===================== Yêu cầu dạy thay =====================

    private String subKey(Long classId, LocalDate date) {
        return SUB_PREFIX + classId + ":" + date;
    }

    @Override
    @Transactional
    public SubstitutionEntry request(Long classId, LocalDate date, Long assistantTutorId, String reason) {
        String key = subKey(classId, date);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        Payload payload = new Payload();
        payload.tutorId = assistantTutorId;
        payload.status = SubstitutionEntry.PENDING;
        payload.reason = reason;
        param.setParamValue(write(payload));
        param.setDescription("Yêu cầu gia sư phụ dạy thay");
        systemParameterRepository.save(param);
        return new SubstitutionEntry(classId, date, assistantTutorId, SubstitutionEntry.PENDING, reason);
    }

    @Override
    @Transactional
    public SubstitutionEntry decide(Long classId, LocalDate date, boolean approve) {
        String key = subKey(classId, date);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu dạy thay"));
        Payload payload = read(param.getParamValue());
        payload.status = approve ? SubstitutionEntry.APPROVED : SubstitutionEntry.REJECTED;
        param.setParamValue(write(payload));
        systemParameterRepository.save(param);
        return toEntry(key, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubstitutionEntry> find(Long classId, LocalDate date) {
        return systemParameterRepository.findByParamKey(subKey(classId, date))
                .map(p -> toEntry(p.getParamKey(), read(p.getParamValue())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubstitutionEntry> listByClassIds(Collection<Long> classIds) {
        Set<Long> ids = new HashSet<>(classIds);
        return systemParameterRepository.findByParamKeyStartingWith(SUB_PREFIX).stream()
                .map(p -> toEntry(p.getParamKey(), read(p.getParamValue())))
                .filter(e -> ids.contains(e.classId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubstitutionEntry> listApprovedByClassIds(Collection<Long> classIds) {
        return listByClassIds(classIds).stream()
                .filter(e -> SubstitutionEntry.APPROVED.equals(e.status()))
                .toList();
    }

    // key = "substi:{classId}:{date}"
    private SubstitutionEntry toEntry(String key, Payload payload) {
        String rest = key.substring(SUB_PREFIX.length());
        int sep = rest.indexOf(':');
        Long classId = Long.valueOf(rest.substring(0, sep));
        LocalDate date = LocalDate.parse(rest.substring(sep + 1));
        return new SubstitutionEntry(classId, date, payload.tutorId, payload.status, payload.reason);
    }

    private Long parseLong(String value) {
        try {
            return value != null ? Long.valueOf(value.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String write(Payload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được dữ liệu dạy thay", e);
        }
    }

    private Payload read(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Payload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Không đọc được dữ liệu dạy thay", e);
        }
    }
}
