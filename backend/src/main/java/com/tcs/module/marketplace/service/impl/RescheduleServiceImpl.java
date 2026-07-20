package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.marketplace.dto.RescheduleEntry;
import com.tcs.module.marketplace.service.RescheduleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RescheduleServiceImpl implements RescheduleService {

    private static final String PREFIX = "resched:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemParameterRepository systemParameterRepository;

    /** Cấu trúc JSON lưu trong param_value. */
    private static final class Payload {
        public String newDate;
        public String newStartTime;
        public String newEndTime;
        public String status;
        public Long tutorId;
        public String reason;
    }

    private String keyOf(Long classId, LocalDate originalDate) {
        return PREFIX + classId + ":" + originalDate;
    }

    @Override
    @Transactional
    public RescheduleEntry request(
            Long classId, LocalDate originalDate, LocalDate newDate,
            LocalTime newStartTime, LocalTime newEndTime, Long tutorId, String reason) {
        String key = keyOf(classId, originalDate);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseGet(SystemParameter::new);
        param.setParamKey(key);
        Payload payload = new Payload();
        payload.newDate = newDate.toString();
        payload.newStartTime = newStartTime != null ? newStartTime.toString() : null;
        payload.newEndTime = newEndTime != null ? newEndTime.toString() : null;
        payload.status = RescheduleEntry.PENDING;
        payload.tutorId = tutorId;
        payload.reason = reason;
        param.setParamValue(write(payload));
        param.setDescription("Yêu cầu dời buổi học");
        systemParameterRepository.save(param);
        return new RescheduleEntry(classId, originalDate, newDate,
                newStartTime, newEndTime, RescheduleEntry.PENDING, tutorId, reason);
    }

    @Override
    @Transactional
    public RescheduleEntry decide(Long classId, LocalDate originalDate, boolean approve) {
        String key = keyOf(classId, originalDate);
        SystemParameter param = systemParameterRepository.findByParamKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu dời lịch"));
        Payload payload = read(param.getParamValue());
        payload.status = approve ? RescheduleEntry.APPROVED : RescheduleEntry.REJECTED;
        param.setParamValue(write(payload));
        systemParameterRepository.save(param);
        return toEntry(key, payload);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RescheduleEntry> find(Long classId, LocalDate originalDate) {
        return systemParameterRepository.findByParamKey(keyOf(classId, originalDate))
                .map(p -> toEntry(p.getParamKey(), read(p.getParamValue())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleEntry> listByClassIds(Collection<Long> classIds) {
        Set<Long> ids = new HashSet<>(classIds);
        return systemParameterRepository.findByParamKeyStartingWith(PREFIX).stream()
                .map(p -> toEntry(p.getParamKey(), read(p.getParamValue())))
                .filter(e -> ids.contains(e.classId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleEntry> listPendingByClassIds(Collection<Long> classIds) {
        return listByClassIds(classIds).stream()
                .filter(e -> RescheduleEntry.PENDING.equals(e.status()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleEntry> listApprovedByClassIds(Collection<Long> classIds) {
        return listByClassIds(classIds).stream()
                .filter(e -> RescheduleEntry.APPROVED.equals(e.status()))
                .toList();
    }

    // key = "resched:{classId}:{originalDate}"
    private RescheduleEntry toEntry(String key, Payload payload) {
        String[] parts = key.split(":");
        Long classId = Long.valueOf(parts[1]);
        LocalDate originalDate = LocalDate.parse(parts[2]);
        return new RescheduleEntry(
                classId,
                originalDate,
                LocalDate.parse(payload.newDate),
                payload.newStartTime != null ? LocalTime.parse(payload.newStartTime) : null,
                payload.newEndTime != null ? LocalTime.parse(payload.newEndTime) : null,
                payload.status,
                payload.tutorId,
                payload.reason);
    }

    private String write(Payload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được dữ liệu dời lịch", e);
        }
    }

    private Payload read(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, Payload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Không đọc được dữ liệu dời lịch", e);
        }
    }
}
