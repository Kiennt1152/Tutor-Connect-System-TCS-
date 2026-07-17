package com.tcs.module.center.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.SaveRecruitmentPostRequest;
import com.tcs.module.center.dto.response.RecruitmentApplicationResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.entity.RecruitmentApplication;
import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.RecruitmentApplicationStatus;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import com.tcs.module.center.repository.RecruitmentApplicationRepository;
import com.tcs.module.center.repository.RecruitmentPostRepository;
import com.tcs.module.center.service.CenterService;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CenterServiceImpl implements CenterService {

    private final AuthHelper authHelper;
    private final RecruitmentPostRepository recruitmentPostRepository;
    private final RecruitmentApplicationRepository recruitmentApplicationRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final LocationRepository locationRepository;
    private final ProvinceRepository provinceRepository;

    // ===================== Phía gia sư / công khai =====================

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentPostResponse> listOpenRecruitmentPosts() {
        return recruitmentPostRepository
                .findByStatusOrderByPublishedAtDesc(RecruitmentPostStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request) {
        Tutor tutor = requireTutor();
        RecruitmentPost post = findPost(recruitmentId);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Tin tuyển dụng chưa mở hoặc đã đóng");
        }
        // Mỗi gia sư chỉ nộp một đơn cho mỗi tin.
        recruitmentApplicationRepository
                .findFirstByRecruitmentPost_RecruitmentIdAndTutor_TutorId(recruitmentId, tutor.getTutorId())
                .ifPresent(a -> {
                    throw new IllegalArgumentException("Bạn đã ứng tuyển tin này rồi");
                });
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentPost(post);
        application.setTutor(tutor);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(RecruitmentApplicationStatus.APPLIED);
        recruitmentApplicationRepository.save(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplicationResponse> listMyApplications() {
        Tutor tutor = requireTutor();
        return recruitmentApplicationRepository
                .findByTutor_TutorIdOrderByAppliedAtDesc(tutor.getTutorId())
                .stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    // ===================== Phía trung tâm =====================

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentPostResponse> listMyRecruitmentPosts() {
        TutorCenter center = requireCenter();
        return recruitmentPostRepository
                .findByCenter_CenterIdOrderByCreatedAtDesc(center.getCenterId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecruitmentPostResponse createRecruitmentPost(SaveRecruitmentPostRequest request) {
        TutorCenter center = requireCenter();
        validate(request);
        RecruitmentPost post = new RecruitmentPost();
        post.setCenter(center);
        post.setStatus(RecruitmentPostStatus.DRAFT); // tin mới luôn là nháp
        applyFields(post, request);
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional
    public RecruitmentPostResponse updateRecruitmentPost(
            Long recruitmentId, SaveRecruitmentPostRequest request) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() != RecruitmentPostStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ tin ở trạng thái nháp mới có thể chỉnh sửa");
        }
        validate(request);
        applyFields(post, request);
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional
    public RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() != RecruitmentPostStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ tin ở trạng thái nháp mới có thể đăng tải");
        }
        post.setStatus(RecruitmentPostStatus.ACTIVE);
        post.setPublishedAt(LocalDateTime.now());
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional
    public RecruitmentPostResponse closeRecruitmentPost(Long recruitmentId) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ tin đang mở mới có thể đóng");
        }
        post.setStatus(RecruitmentPostStatus.CLOSED);
        post.setClosedAt(LocalDateTime.now());
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentApplicationResponse> listApplications(Long recruitmentId) {
        requireCenter();
        RecruitmentPost post = findPost(recruitmentId);
        requireOwner(post); // chỉ xem đơn của tin do mình đăng
        return recruitmentApplicationRepository
                .findByRecruitmentPost_RecruitmentIdOrderByAppliedAtDesc(recruitmentId)
                .stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    @Override
    @Transactional
    public RecruitmentApplicationResponse decideApplication(Long recruitmentAppId, boolean approve) {
        requireCenter();
        RecruitmentApplication application = recruitmentApplicationRepository
                .findById(recruitmentAppId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
        requireOwner(application.getRecruitmentPost());
        if (application.getStatus() != RecruitmentApplicationStatus.APPLIED) {
            throw new IllegalArgumentException("Đơn này đã được xử lý");
        }
        application.setStatus(
                approve ? RecruitmentApplicationStatus.HIRED : RecruitmentApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        return toApplicationResponse(recruitmentApplicationRepository.save(application));
    }

    // ===================== Helper =====================

    private void validate(SaveRecruitmentPostRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề là bắt buộc");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Mô tả công việc là bắt buộc");
        }
        if (request.getMaxPositions() != null && request.getMaxPositions() <= 0) {
            throw new IllegalArgumentException("Số lượng cần tuyển phải là số nguyên dương");
        }
        if (request.getRequiredExperience() != null && request.getRequiredExperience() < 0) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không được âm");
        }
        // Địa điểm là tuỳ chọn, nhưng đã nhập địa chỉ thì phải có tỉnh/thành (Location cần cả hai).
        if (StringUtils.hasText(request.getAddressDetail())
                && !StringUtils.hasText(request.getProvinceName())) {
            throw new IllegalArgumentException("Vui lòng chọn Tỉnh/Thành phố cho địa chỉ đã nhập");
        }
    }

    private void applyFields(RecruitmentPost post, SaveRecruitmentPostRequest request) {
        post.setTitle(request.getTitle().trim());
        post.setDescription(request.getDescription().trim());
        post.setRequirements(request.getRequirements());
        post.setBenefits(request.getBenefits());
        post.setRequiredExperience(
                request.getRequiredExperience() != null ? request.getRequiredExperience() : 0);
        post.setMaxPositions(request.getMaxPositions() != null ? request.getMaxPositions() : 1);
        post.setSubject(
                StringUtils.hasText(request.getSubjectName())
                        ? resolveOrCreateSubject(request.getSubjectName())
                        : null);
        post.setLocation(
                StringUtils.hasText(request.getProvinceName())
                                && StringUtils.hasText(request.getAddressDetail())
                        ? resolveOrCreateLocation(
                                request.getProvinceName(),
                                request.getWardName(),
                                request.getAddressDetail())
                        : null);
    }

    // Tìm-hoặc-tạo theo tên người dùng tự nhập (giữ FK toàn vẹn mà không cần dropdown).
    private Subject resolveOrCreateSubject(String name) {
        String n = name.trim();
        return subjectRepository.findFirstBySubjectNameIgnoreCase(n).orElseGet(() -> {
            Subject s = new Subject();
            s.setSubjectName(n);
            return subjectRepository.save(s);
        });
    }

    // Mô hình 2 cấp: address_line lưu địa chỉ cụ thể; ward_name/province lưu cấp hành chính.
    private Location resolveOrCreateLocation(String provinceName, String wardName, String addressDetail) {
        Location loc = new Location();
        loc.setAddressLine(addressDetail.trim());
        loc.setWardName(StringUtils.hasText(wardName) ? wardName.trim() : null);
        loc.setDistrictName(null);
        loc.setProvince(resolveOrCreateProvince(provinceName));
        return locationRepository.save(loc);
    }

    private Province resolveOrCreateProvince(String name) {
        String n = name.trim();
        return provinceRepository.findFirstByProvinceNameIgnoreCase(n).orElseGet(() -> {
            Province p = new Province();
            p.setProvinceName(n);
            return provinceRepository.save(p);
        });
    }

    private TutorCenter requireCenter() {
        authHelper.requireRole(UserRole.TUTOR_CENTER);
        return tutorCenterRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ trung tâm"));
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    /** Chỉ trung tâm sở hữu tin mới được thao tác. */
    private void requireOwner(RecruitmentPost post) {
        if (!post.getCenter().getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Bạn không có quyền với tin tuyển dụng này");
        }
    }

    private RecruitmentPost findPost(Long recruitmentId) {
        return recruitmentPostRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
    }

    private String locationLabel(Location location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(location.getAddressLine());
        if (StringUtils.hasText(location.getWardName())) {
            sb.append(", ").append(location.getWardName());
        }
        if (location.getProvince() != null
                && StringUtils.hasText(location.getProvince().getProvinceName())) {
            sb.append(", ").append(location.getProvince().getProvinceName());
        }
        return sb.toString();
    }

    private RecruitmentPostResponse toResponse(RecruitmentPost post) {
        Location location = post.getLocation();
        Subject subject = post.getSubject();
        return RecruitmentPostResponse.builder()
                .recruitmentId(post.getRecruitmentId())
                .centerId(post.getCenter().getCenterId())
                .centerName(post.getCenter().getCompanyName())
                .title(post.getTitle())
                .description(post.getDescription())
                .requirements(post.getRequirements())
                .benefits(post.getBenefits())
                .requiredExperience(post.getRequiredExperience())
                .maxPositions(post.getMaxPositions())
                .subjectId(subject != null ? subject.getSubjectId() : null)
                .subjectName(subject != null ? subject.getSubjectName() : null)
                .locationId(location != null ? location.getLocationId() : null)
                .locationLabel(locationLabel(location))
                .provinceName(location != null && location.getProvince() != null
                        ? location.getProvince().getProvinceName() : null)
                .wardName(location != null ? location.getWardName() : null)
                .addressDetail(location != null ? location.getAddressLine() : null)
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .closedAt(post.getClosedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .applicationCount(
                        recruitmentApplicationRepository.countByRecruitmentPost_RecruitmentId(
                                post.getRecruitmentId()))
                .build();
    }

    private RecruitmentApplicationResponse toApplicationResponse(RecruitmentApplication application) {
        Tutor tutor = application.getTutor();
        RecruitmentPost post = application.getRecruitmentPost();
        return RecruitmentApplicationResponse.builder()
                .recruitmentAppId(application.getRecruitmentAppId())
                .recruitmentId(post.getRecruitmentId())
                .postTitle(post.getTitle())
                .centerName(post.getCenter().getCompanyName())
                .tutorId(tutor.getTutorId())
                .tutorName(tutor.getFullName())
                .tutorPhone(tutor.getPhone())
                .tutorAvatar(tutor.getAvatar())
                .experienceYears(tutor.getExperienceYears())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(
                        tutor.getVerificationStatus() != null
                                ? tutor.getVerificationStatus().name() : null)
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }
}
