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
import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.ProfileResponse;
import com.tcs.module.profile.dto.response.TutorAvailabilityResponse;
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
import com.tcs.module.profile.repository.TutorExperienceRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.profile.service.ProfileService;
import com.tcs.security.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

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
    private final VerificationService verificationService;
    private final PlatformMapper platformMapper;

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
        return toExperienceResponse(tutorExperienceRepository.save(exp));
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
        return toAvailabilityResponse(tutorAvailabilityRepository.save(availability));
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
                .phone(ctx.user().getPhone());
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
                    .verificationStatus(ctx.tutor().getVerificationStatus());
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
