package com.tcs.module.center.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
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

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentPostResponse> listRecruitmentPosts() {
        return recruitmentPostRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public RecruitmentPostResponse createRecruitmentPost(CreateRecruitmentPostRequest request) {
        TutorCenter center = requireCenter();
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getDescription())) {
            throw new IllegalArgumentException("Tiêu đề và mô tả là bắt buộc");
        }
        RecruitmentPost post = new RecruitmentPost();
        post.setCenter(center);
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setRequirements(request.getRequirements());
        post.setBenefits(request.getBenefits());
        post.setRequiredExperience(request.getRequiredExperience() != null ? request.getRequiredExperience() : 0);
        post.setMaxPositions(request.getMaxPositions() != null ? request.getMaxPositions() : 1);
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository
                    .findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
            post.setSubject(subject);
        }
        if (request.getLocationId() != null) {
            Location location = locationRepository
                    .findById(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm"));
            post.setLocation(location);
        }
        post.setStatus(RecruitmentPostStatus.DRAFT);
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional
    public RecruitmentPostResponse publishRecruitmentPost(Long recruitmentId) {
        RecruitmentPost post = findPost(recruitmentId);
        if (!post.getCenter().getUser().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền đăng tin tuyển dụng này");
        }
        post.setStatus(RecruitmentPostStatus.ACTIVE);
        post.setPublishedAt(LocalDateTime.now());
        return toResponse(recruitmentPostRepository.save(post));
    }

    @Override
    @Transactional
    public void applyToRecruitment(Long recruitmentId, ApplyRecruitmentRequest request) {
        Tutor tutor = requireTutor();
        RecruitmentPost post = findPost(recruitmentId);
        if (post.getStatus() != RecruitmentPostStatus.ACTIVE) {
            throw new IllegalArgumentException("Tin tuyển dụng chưa mở");
        }
        RecruitmentApplication application = new RecruitmentApplication();
        application.setRecruitmentPost(post);
        application.setTutor(tutor);
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(RecruitmentApplicationStatus.APPLIED);
        recruitmentApplicationRepository.save(application);
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

    private RecruitmentPost findPost(Long recruitmentId) {
        return recruitmentPostRepository
                .findById(recruitmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin tuyển dụng"));
    }

    private RecruitmentPostResponse toResponse(RecruitmentPost post) {
        return RecruitmentPostResponse.builder()
                .recruitmentId(post.getRecruitmentId())
                .centerId(post.getCenter().getCenterId())
                .centerName(post.getCenter().getCompanyName())
                .title(post.getTitle())
                .description(post.getDescription())
                .maxPositions(post.getMaxPositions())
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
