package com.tcs.module.profile.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.service.VerificationService;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
import com.tcs.module.profile.dto.response.TutorCertificateResponse;
import com.tcs.module.profile.dto.response.TutorEducationResponse;
import com.tcs.module.profile.dto.response.TutorExperienceResponse;
import com.tcs.module.profile.entity.ChildProfile;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.ParentChildLink;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorAvailability;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.profile.entity.TutorExperience;
import com.tcs.module.profile.enums.ParentChildLinkStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ChildProfileRepository;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.ParentChildLinkRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorAvailabilityRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorCertificateRepository;
import com.tcs.module.profile.repository.TutorEducationRepository;
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.ProfileService;
import com.tcs.security.AuthHelper;
import com.tcs.util.FileMagicDetector;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of(
            FileMagicDetector.MIME_JPEG,
            FileMagicDetector.MIME_PNG,
            FileMagicDetector.MIME_WEBP,
            FileMagicDetector.MIME_GIF
    );

    @Value("${tcs.file.storage.path:uploads}")
    private String storagePath;

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ParentChildLinkRepository parentChildLinkRepository;
    private final GradeRepository gradeRepository;
    private final TutorExperienceRepository tutorExperienceRepository;
    private final TutorAvailabilityRepository tutorAvailabilityRepository;
    private final TutorEducationRepository tutorEducationRepository;
    private final TutorCertificateRepository tutorCertificateRepository;
    private final VerificationService verificationService;
    private final PlatformMapper platformMapper;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        return toProfileResponse(loadContext());
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        ProfileContext ctx = loadContext();
        switch (ctx.role()) {
            case CLIENT -> updateClient(ctx.client(), request);
            case TUTOR -> updateTutor(ctx.tutor(), request);
            case TUTOR_CENTER -> updateCenter(ctx.center(), request);
            default -> throw new ForbiddenException("Không thể cập nhật hồ sơ cho vai trò này");
        }
        // UC-08 BR-UC08-01: lan dau luu ho so thanh cong -> danh dau da hoan tat, an banner onboarding.
        if (ctx.user().getProfileCompletedAt() == null) {
            ctx.user().setProfileCompletedAt(LocalDateTime.now());
            userRepository.save(ctx.user());
        }
        auditLogService.record(ctx.user().getUserId(), "UPDATE_PROFILE", ctx.role().name(),
                ctx.user().getUserId(), null, request);
        return toProfileResponse(ctx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChildProfileResponse> getMyChildren() {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        return parentChildLinkRepository.findByParentUser_UserId(ctx.user().getUserId()).stream()
                .map(link -> toChildResponse(link.getChildProfile()))
                .toList();
    }

    @Override
    @Transactional
    public ChildProfileResponse createChild(ChildProfileRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        ChildProfile child = new ChildProfile();
        applyChildFields(child, request);
        ChildProfile saved = childProfileRepository.save(child);

        ParentChildLink link = new ParentChildLink();
        link.setParentUser(ctx.user());
        link.setChildProfile(saved);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        parentChildLinkRepository.save(link);
        auditLogService.record(ctx.user().getUserId(), "CREATE_CHILD_PROFILE", "ChildProfile",
                saved.getChildProfileId(), null, request);
        return toChildResponse(saved);
    }

    @Override
    @Transactional
    public ChildProfileResponse linkChild(LinkChildRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        ChildProfile child = childProfileRepository
                .findById(request.getChildProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ con"));
        ParentChildLink link = new ParentChildLink();
        link.setParentUser(ctx.user());
        link.setChildProfile(child);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        parentChildLinkRepository.save(link);
        auditLogService.record(ctx.user().getUserId(), "LINK_CHILD_PROFILE", "ChildProfile",
                child.getChildProfileId(), null, request);
        return toChildResponse(child);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorExperienceResponse> getMyExperiences() {
        Tutor tutor = requireTutor(loadContext());
        return tutorExperienceRepository.findByTutor_TutorId(tutor.getTutorId()).stream()
                .map(this::toExperienceResponse)
                .toList();
    }

    @Override
    @Transactional
    public TutorExperienceResponse addExperience(TutorExperienceRequest request) {
        Tutor tutor = requireTutor(loadContext());
        if (!StringUtils.hasText(request.getRole()) || !StringUtils.hasText(request.getOrganization())) {
            throw new IllegalArgumentException("Chức danh và tổ chức là bắt buộc");
        }
        TutorExperience exp = new TutorExperience();
        exp.setTutor(tutor);
        exp.setRole(request.getRole());
        exp.setOrganization(request.getOrganization());
        exp.setStartDate(request.getStartDate());
        exp.setEndDate(request.getEndDate());
        exp.setDescription(request.getDescription());
        TutorExperience saved = tutorExperienceRepository.save(exp);
        auditLogService.record(tutor.getUser().getUserId(), "ADD_EXPERIENCE", "TutorExperience",
                saved.getExperienceId(), null, request);
        return toExperienceResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExperience(Long experienceId) {
        Tutor tutor = requireTutor(loadContext());
        TutorExperience exp = tutorExperienceRepository
                .findById(experienceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kinh nghiệm"));
        if (!exp.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Không có quyền xóa kinh nghiệm này");
        }
        tutorExperienceRepository.delete(exp);
        auditLogService.record(tutor.getUser().getUserId(), "DELETE_EXPERIENCE", "TutorExperience",
                experienceId, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorAvailabilityResponse> getMyAvailability() {
        Tutor tutor = requireTutor(loadContext());
        return tutorAvailabilityRepository.findByTutor_TutorId(tutor.getTutorId()).stream()
                .map(this::toAvailabilityResponse)
                .toList();
    }

    @Override
    @Transactional
    public TutorAvailabilityResponse addAvailability(TutorAvailabilityRequest request) {
        Tutor tutor = requireTutor(loadContext());
        if (request.getDayOfWeek() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("Ngày và khung giờ là bắt buộc");
        }
        TutorAvailability availability = new TutorAvailability();
        availability.setTutor(tutor);
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setRecurring(request.getRecurring() != null ? request.getRecurring() : true);
        availability.setSpecificDate(request.getSpecificDate());
        TutorAvailability saved = tutorAvailabilityRepository.save(availability);
        auditLogService.record(tutor.getUser().getUserId(), "ADD_AVAILABILITY", "TutorAvailability",
                saved.getAvailabilityId(), null, request);
        return toAvailabilityResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAvailability(Long availabilityId) {
        Tutor tutor = requireTutor(loadContext());
        TutorAvailability availability = tutorAvailabilityRepository
                .findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch rảnh"));
        if (!availability.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Không có quyền xóa lịch này");
        }
        tutorAvailabilityRepository.delete(availability);
        auditLogService.record(tutor.getUser().getUserId(), "DELETE_AVAILABILITY", "TutorAvailability",
                availabilityId, null, null);
    }

    @Override
    @Transactional
    public com.tcs.module.identity.dto.response.VerificationResponse submitVerification(
            com.tcs.module.identity.dto.request.VerificationRequestDto request
    ) {
        ProfileContext ctx = loadContext();
        if (ctx.role() != UserRole.TUTOR && ctx.role() != UserRole.TUTOR_CENTER) {
            throw new ForbiddenException("Chỉ gia sư hoặc trung tâm mới nộp xác minh");
        }
        if (request.getVerificationType() == null) {
            request.setVerificationType(ctx.role() == UserRole.TUTOR
                    ? com.tcs.module.identity.enums.VerificationType.TUTOR_PROFILE
                    : com.tcs.module.identity.enums.VerificationType.TUTOR_CENTER_LICENSE);
        }
        return verificationService.submitVerification(request);
    }

    @Override
    @Transactional
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Kích thước ảnh không được vượt quá 5MB");
        }

        String detectedMime = detectAvatarMime(file);
        if (!ALLOWED_AVATAR_TYPES.contains(detectedMime)) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WEBP, GIF)");
        }
        String extension = FileMagicDetector.extensionFor(detectedMime);

        ProfileContext ctx = loadContext();
        String fileName = "avatars/user-" + ctx.user().getUserId() + extension;
        Path avatarPath = Paths.get(storagePath).toAbsolutePath().normalize().resolve(fileName);

        try {
            Files.createDirectories(avatarPath.getParent());
            Files.copy(file.getInputStream(), avatarPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu ảnh đại diện", e);
        }

        String avatarUrl = "/uploads/" + fileName;
        switch (ctx.role()) {
            case CLIENT -> {
                ctx.client().setAvatarUrl(avatarUrl);
                clientRepository.save(ctx.client());
            }
            case TUTOR -> {
                ctx.tutor().setAvatar(avatarUrl);
                tutorRepository.save(ctx.tutor());
            }
            case TUTOR_CENTER -> {
                ctx.center().setAvatar(avatarUrl);
                tutorCenterRepository.save(ctx.center());
            }
            default -> log.warn("uploadAvatar called with unsupported role: {}", ctx.role());
        }
        auditLogService.record(ctx.user().getUserId(), "UPLOAD_AVATAR", ctx.role().name(),
                ctx.user().getUserId(), null, java.util.Map.of("avatarUrl", avatarUrl));
        return avatarUrl;
    }

    private String detectAvatarMime(MultipartFile file) {
        try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream())) {
            String detected = FileMagicDetector.detect(bis);
            if (detected == null) {
                throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPEG, PNG, WEBP, GIF)");
            }
            return detected;
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file ảnh", e);
        }
    }

    private ProfileContext loadContext() {
        Long userId = authHelper.currentUserId();
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        UserProfileBundle profiles = UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
        UserRole role = platformMapper.resolveRole(profiles);
        return new ProfileContext(user, role, profiles.client(), profiles.tutor(), profiles.tutorCenter());
    }

    private void requireRole(ProfileContext ctx, UserRole role) {
        if (ctx.role() != role) {
            throw new ForbiddenException("Không có quyền truy cập");
        }
    }

    private Tutor requireTutor(ProfileContext ctx) {
        requireRole(ctx, UserRole.TUTOR);
        if (ctx.tutor() == null) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư");
        }
        return ctx.tutor();
    }

    private void updateClient(Client client, UpdateProfileRequest request) {
        if (StringUtils.hasText(request.getFullName())) client.setFullName(request.getFullName());
        if (StringUtils.hasText(request.getPhone())) client.setPhone(request.getPhone());
        if (request.getAddress() != null) client.setAddress(request.getAddress());
        if (request.getAvatarUrl() != null) client.setAvatarUrl(request.getAvatarUrl());
        if (request.getDateOfBirth() != null) client.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) client.setGender(request.getGender());
        clientRepository.save(client);
    }

    private void updateTutor(Tutor tutor, UpdateProfileRequest request) {
        if (StringUtils.hasText(request.getFullName())) tutor.setFullName(request.getFullName());
        if (StringUtils.hasText(request.getPhone())) tutor.setPhone(request.getPhone());
        if (request.getAddress() != null) tutor.setAddress(request.getAddress());
        if (request.getAvatarUrl() != null) tutor.setAvatar(request.getAvatarUrl());
        if (request.getDateOfBirth() != null) tutor.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) tutor.setGender(request.getGender());
        if (request.getBio() != null) tutor.setBio(request.getBio());
        if (request.getExperienceYears() != null) tutor.setExperienceYears(request.getExperienceYears());
        if (request.getHourlyRate() != null) tutor.setHourlyRate(request.getHourlyRate());
        tutorRepository.save(tutor);
    }

    private void updateCenter(TutorCenter center, UpdateProfileRequest request) {
        if (StringUtils.hasText(request.getCompanyName())) center.setCompanyName(request.getCompanyName());
        if (StringUtils.hasText(request.getPhone())) center.setPhone(request.getPhone());
        if (request.getAddress() != null) center.setAddress(request.getAddress());
        if (request.getAvatarUrl() != null) center.setAvatar(request.getAvatarUrl());
        if (request.getDescription() != null) center.setDescription(request.getDescription());
        tutorCenterRepository.save(center);
    }

    private void applyChildFields(ChildProfile child, ChildProfileRequest request) {
        if (!StringUtils.hasText(request.getFullName())) {
            throw new IllegalArgumentException("Tên con là bắt buộc");
        }
        child.setFullName(request.getFullName());
        child.setDateOfBirth(request.getDateOfBirth());
        child.setGender(request.getGender());
        child.setSchoolName(request.getSchoolName());
        child.setNotes(request.getNotes());
        if (request.getGradeId() != null) {
            Grade grade = gradeRepository
                    .findById(request.getGradeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối/lớp"));
            child.setGrade(grade);
        }
    }

    private ProfileResponse toProfileResponse(ProfileContext ctx) {
        ProfileResponse.ProfileResponseBuilder builder = ProfileResponse.builder()
                .userId(ctx.user().getUserId())
                .role(ctx.role())
                .email(ctx.user().getEmail())
                .phone(ctx.user().getPhone())
                // UC-08 BR-UC08-01: re-evaluate tren /api/profile/me de banner onboarding tu an sau khi luu.
                .firstLogin(ctx.user().getProfileCompletedAt() == null);
        if (ctx.client() != null) {
            builder.fullName(ctx.client().getFullName())
                    .phone(ctx.client().getPhone())
                    .address(ctx.client().getAddress())
                    .avatarUrl(ctx.client().getAvatarUrl())
                    .dateOfBirth(ctx.client().getDateOfBirth())
                    .gender(ctx.client().getGender());
        }
        if (ctx.tutor() != null) {
            builder.fullName(ctx.tutor().getFullName())
                    .phone(ctx.tutor().getPhone())
                    .address(ctx.tutor().getAddress())
                    .avatarUrl(ctx.tutor().getAvatar())
                    .dateOfBirth(ctx.tutor().getDateOfBirth())
                    .gender(ctx.tutor().getGender())
                    .bio(ctx.tutor().getBio())
                    .experienceYears(ctx.tutor().getExperienceYears())
                    .hourlyRate(ctx.tutor().getHourlyRate())
                    .verificationStatus(ctx.tutor().getVerificationStatus())
                    .educations(tutorEducationRepository
                            .findByTutor_TutorId(ctx.tutor().getTutorId()).stream()
                            .map(this::toEducationResponse)
                            .toList())
                    .certificates(tutorCertificateRepository
                            .findByTutor_TutorId(ctx.tutor().getTutorId()).stream()
                            .map(this::toCertificateResponse)
                            .toList());
        }
        if (ctx.center() != null) {
            builder.fullName(ctx.center().getCompanyName())
                    .companyName(ctx.center().getCompanyName())
                    .licenseNo(ctx.center().getLicenseNo())
                    .phone(ctx.center().getPhone())
                    .address(ctx.center().getAddress())
                    .avatarUrl(ctx.center().getAvatar())
                    .description(ctx.center().getDescription())
                    .verificationStatus(ctx.center().getVerificationStatus());
        }
        return builder.build();
    }

    private ChildProfileResponse toChildResponse(ChildProfile child) {
        return ChildProfileResponse.builder()
                .childProfileId(child.getChildProfileId())
                .fullName(child.getFullName())
                .dateOfBirth(child.getDateOfBirth())
                .gender(child.getGender())
                .gradeId(child.getGrade() != null ? child.getGrade().getGradeId() : null)
                .gradeName(child.getGrade() != null ? child.getGrade().getGradeName() : null)
                .schoolName(child.getSchoolName())
                .notes(child.getNotes())
                .createdAt(child.getCreatedAt())
                .build();
    }

    private TutorExperienceResponse toExperienceResponse(TutorExperience exp) {
        return TutorExperienceResponse.builder()
                .experienceId(exp.getExperienceId())
                .role(exp.getRole())
                .organization(exp.getOrganization())
                .startDate(exp.getStartDate())
                .endDate(exp.getEndDate())
                .description(exp.getDescription())
                .build();
    }

    private TutorEducationResponse toEducationResponse(
            com.tcs.module.profile.entity.TutorEducation e) {
        return TutorEducationResponse.builder()
                .educationId(e.getEducationId())
                .institution(e.getInstitution())
                .degree(e.getDegree())
                .fieldOfStudy(e.getFieldOfStudy())
                .startYear(e.getStartYear())
                .endYear(e.getEndYear())
                .build();
    }

    private TutorCertificateResponse toCertificateResponse(
            com.tcs.module.profile.entity.TutorCertificate c) {
        return TutorCertificateResponse.builder()
                .certificateId(c.getCertificateId())
                .name(c.getName())
                .issuer(c.getIssuer())
                .issueDate(c.getIssueDate())
                .build();
    }

    private TutorAvailabilityResponse toAvailabilityResponse(TutorAvailability a) {
        return TutorAvailabilityResponse.builder()
                .availabilityId(a.getAvailabilityId())
                .dayOfWeek(a.getDayOfWeek())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .recurring(a.getRecurring())
                .specificDate(a.getSpecificDate())
                .googleCalendarEventId(a.getGoogleCalendarEventId())
                .build();
    }

    private record ProfileContext(
            User user, UserRole role, Client client, Tutor tutor, TutorCenter center) {}
}
