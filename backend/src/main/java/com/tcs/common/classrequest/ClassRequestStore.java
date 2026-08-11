package com.tcs.common.classrequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.dto.response.CandidateTutorResponse;
import com.tcs.module.marketplace.dto.response.ClassRequestResponse;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
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
    public static final String STATUS_PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CANCELLED = "CANCELLED";
    /** Trung tâm đã nhận tìm gia sư (đang tìm nguồn). */
    public static final String STATUS_SEARCHING = "SEARCHING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    // Tự tạo ObjectMapper riêng: dự án không expose bean ObjectMapper để inject.
    // createdAt lưu dạng chuỗi nên không cần JavaTimeModule.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SystemParameterRepository systemParameterRepository;
    private final CategoryRepository categoryRepository;
    private final ClientRepository clientRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutorRepository tutorRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;

    /**
     * Dữ liệu một yêu cầu mở lớp (createdAt lưu dạng chuỗi để không cần Jackson time-module).
     * {@code detailsJson} chứa nguyên payload form "tìm gia sư" của phụ huynh (môn, lịch, địa điểm…)
     * để trung tâm xem đầy đủ và dùng lại khi tạo lớp lúc phụ huynh chọn gia sư.
     */
    public record ClassRequestData(
            String requestId,
            Long clientUserId,
            Long centerId,
            Long categoryId,
            String note,
            BigDecimal desiredBudget,
            String status,
            String reason,
            String createdAt,
            String detailsJson,
            List<Long> candidateTutorIds,
            Long recruitmentPostId) {}

    public ClassRequestData create(
            Long clientUserId, Long centerId, Long categoryId, String note, BigDecimal desiredBudget,
            String detailsJson) {
        ClassRequestData data = new ClassRequestData(
                UUID.randomUUID().toString(), clientUserId, centerId, categoryId, note, desiredBudget,
                STATUS_PAYMENT_PENDING, null, LocalDateTime.now().toString(), detailsJson, new ArrayList<>(),
                null);
        save(data);
        return data;
    }

    /** Trả về bản sao với trạng thái/lý do mới (giữ nguyên các trường còn lại). */
    public ClassRequestData withStatus(ClassRequestData d, String status, String reason) {
        return new ClassRequestData(
                d.requestId(), d.clientUserId(), d.centerId(), d.categoryId(), d.note(),
                d.desiredBudget(), status, reason, d.createdAt(), d.detailsJson(),
                candidatesOf(d), d.recruitmentPostId());
    }

    /** Trả về bản sao với danh sách ứng viên mới. */
    public ClassRequestData withCandidates(ClassRequestData d, List<Long> candidateTutorIds) {
        return new ClassRequestData(
                d.requestId(), d.clientUserId(), d.centerId(), d.categoryId(), d.note(),
                d.desiredBudget(), d.status(), d.reason(), d.createdAt(), d.detailsJson(),
                candidateTutorIds == null ? new ArrayList<>() : candidateTutorIds,
                d.recruitmentPostId());
    }

    /** Trả về bản sao gắn (hoặc gỡ) tin tuyển dụng đã đăng cho yêu cầu này. */
    public ClassRequestData withRecruitmentPost(ClassRequestData d, Long recruitmentPostId) {
        return new ClassRequestData(
                d.requestId(), d.clientUserId(), d.centerId(), d.categoryId(), d.note(),
                d.desiredBudget(), d.status(), d.reason(), d.createdAt(), d.detailsJson(),
                candidatesOf(d), recruitmentPostId);
    }

    /** Tìm yêu cầu đang gắn với một tin tuyển dụng (dùng khi duyệt đơn/đóng tin). */
    public Optional<ClassRequestData> findByRecruitmentPostId(Long recruitmentPostId) {
        if (recruitmentPostId == null) {
            return Optional.empty();
        }
        return all().stream()
                .filter(d -> recruitmentPostId.equals(d.recruitmentPostId()))
                .findFirst();
    }

    /** Danh sách ứng viên (không null) của một yêu cầu. */
    public List<Long> candidatesOf(ClassRequestData d) {
        return d.candidateTutorIds() == null ? new ArrayList<>() : new ArrayList<>(d.candidateTutorIds());
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
                .detailsJson(d.detailsJson())
                .candidates(resolveCandidates(d))
                .recruitmentPostId(d.recruitmentPostId())
                .build();
    }

    /** Nạp thông tin gia sư được đề cử (shortlist) để phụ huynh/trung tâm hiển thị. */
    private List<CandidateTutorResponse> resolveCandidates(ClassRequestData d) {
        List<CandidateTutorResponse> out = new ArrayList<>();
        for (Long tutorId : candidatesOf(d)) {
            tutorRepository.findById(tutorId).ifPresent(t -> out.add(
                    CandidateTutorResponse.builder()
                            .tutorId(t.getTutorId())
                            .fullName(t.getFullName())
                            .experienceYears(t.getExperienceYears())
                            .ratingAvg(t.getRatingAvg())
                            .certificates(loadCertificates(t))
                            .build()));
        }
        return out;
    }

    /** Bằng cấp / chứng chỉ đã xác minh (chỉ loại CERTIFICATE) của gia sư — để phụ huynh xem trước. */
    private List<CandidateTutorResponse.CertificateInfo> loadCertificates(Tutor tutor) {
        if (tutor.getUser() == null) {
            return List.of();
        }
        return verificationRequestRepository
                .findFirstByUser_UserIdAndVerificationTypeAndStatusOrderByVerificationIdDesc(
                        tutor.getUser().getUserId(), VerificationType.TUTOR_PROFILE,
                        VerificationStatus.VERIFIED)
                .map(req -> verificationDocumentRepository
                        .findByVerificationRequest_VerificationId(req.getVerificationId()).stream()
                        .filter(doc -> doc.getDocumentType() == VerificationDocumentType.CERTIFICATE)
                        .map(doc -> CandidateTutorResponse.CertificateInfo.builder()
                                .documentType(doc.getDocumentType() == null
                                        ? null : doc.getDocumentType().name())
                                .fileName(doc.getFile().getFileName())
                                .fileUrl(doc.getFile().getFileUrl())
                                .mimeType(doc.getFile().getMimeType())
                                .fileSize(doc.getFile().getFileSize())
                                .build())
                        .toList())
                .orElseGet(List::of);
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
