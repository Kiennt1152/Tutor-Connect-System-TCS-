package com.tcs.common.classrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Lưu "yêu cầu mở lớp" của phụ huynh gửi tới một trung tâm dưới dạng JSON trong
 * {@code system_parameters} (không thêm bảng/migration). Khi trung tâm CHẤP NHẬN mới sinh lớp
 * EXTERNAL thật — tái dùng toàn bộ vòng đời lớp "yêu cầu ngoài".
 *
 * <p>Mỗi yêu cầu = 1 dòng key {@code classreq:{uuid}}, value là JSON của {@link ClassRequestData}.
 */
@Component
@RequiredArgsConstructor
public class ClassRequestStore {

    public static final String PREFIX = "classreq:";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    // Tự tạo ObjectMapper riêng: dự án không expose bean ObjectMapper để inject.
    // createdAt lưu dạng chuỗi nên không cần JavaTimeModule.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemParameterRepository systemParameterRepository;
    private final CategoryRepository categoryRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;

    /** Dữ liệu một yêu cầu mở lớp (createdAt lưu dạng chuỗi để không cần Jackson time-module). */
    public record ClassRequestData(
            String requestId,
            Long clientUserId,
            Long centerId,
            Long categoryId,
            String note,
            BigDecimal desiredBudget,
            String status,
            String reason,
            String createdAt) {}

    public ClassRequestData create(
            Long clientUserId, Long centerId, Long categoryId, String note, BigDecimal desiredBudget) {
        ClassRequestData data = new ClassRequestData(
                UUID.randomUUID().toString(), clientUserId, centerId, categoryId, note, desiredBudget,
                STATUS_PENDING, null, LocalDateTime.now().toString());
        save(data);
        return data;
    }

    public Optional<ClassRequestData> find(String requestId) {
        return systemParameterRepository.findByParamKey(PREFIX + requestId).map(this::parse);
    }

    public List<ClassRequestData> findByCenter(Long centerId) {
        return all().stream().filter(d -> centerId.equals(d.centerId())).toList();
    }

    public List<ClassRequestData> findByClient(Long clientUserId) {
        return all().stream().filter(d -> clientUserId.equals(d.clientUserId())).toList();
    }

    public void save(ClassRequestData data) {
        SystemParameter param = systemParameterRepository
                .findByParamKey(PREFIX + data.requestId())
                .orElseGet(SystemParameter::new);
        param.setParamKey(PREFIX + data.requestId());
        param.setParamValue(write(data));
        param.setDescription("Yeu cau mo lop cua phu huynh");
        systemParameterRepository.save(param);
    }

    public void delete(String requestId) {
        systemParameterRepository.findByParamKey(PREFIX + requestId)
                .ifPresent(systemParameterRepository::delete);
    }

    /** Chuyển dữ liệu thô thành response (nạp thêm tên trung tâm / phụ huynh / danh mục). */
    public ClassRequestResponse toResponse(ClassRequestData d) {
        String centerName = tutorCenterRepository.findById(d.centerId())
                .map(TutorCenter::getCompanyName).orElse(null);
        String clientName = clientRepository.findByUser_UserId(d.clientUserId())
                .map(Client::getFullName).orElse(null);
        String categoryName = d.categoryId() == null ? null
                : categoryRepository.findById(d.categoryId()).map(Category::getName).orElse(null);
        return ClassRequestResponse.builder()
                .requestId(d.requestId())
                .centerId(d.centerId())
                .centerName(centerName)
                .clientUserId(d.clientUserId())
                .clientName(clientName)
                .categoryId(d.categoryId())
                .categoryName(categoryName)
                .note(d.note())
                .desiredBudget(d.desiredBudget())
                .status(d.status())
                .reason(d.reason())
                .createdAt(d.createdAt())
                .build();
    }

    private List<ClassRequestData> all() {
        List<ClassRequestData> out = new ArrayList<>();
        for (SystemParameter p : systemParameterRepository.findByParamKeyStartingWith(PREFIX)) {
            out.add(parse(p));
        }
        return out;
    }

    private ClassRequestData parse(SystemParameter p) {
        try {
            return OBJECT_MAPPER.readValue(p.getParamValue(), ClassRequestData.class);
        } catch (Exception e) {
            throw new IllegalStateException("Dữ liệu yêu cầu mở lớp hỏng: " + p.getParamKey(), e);
        }
    }

    private String write(ClassRequestData data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được yêu cầu mở lớp", e);
        }
    }
}
