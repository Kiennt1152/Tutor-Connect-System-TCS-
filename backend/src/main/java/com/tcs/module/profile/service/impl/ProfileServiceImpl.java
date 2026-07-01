package com.tcs.module.profile.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.profile.dto.request.ChildProfileRequest;
import com.tcs.module.profile.dto.request.LinkChildAccountRequest;
import com.tcs.module.profile.dto.request.LinkChildRequest;
import com.tcs.module.profile.dto.request.LinkGuardianRequest;
import com.tcs.module.profile.dto.request.TutorAvailabilityRequest;
import com.tcs.module.profile.dto.request.TutorExperienceRequest;
import com.tcs.module.profile.dto.request.UpdateChildProfileRequest;
import com.tcs.module.profile.dto.request.UpdateProfileRequest;
import com.tcs.module.profile.dto.response.ChildProfileResponse;
import com.tcs.module.profile.dto.response.DependentLinkStatusResponse;
import com.tcs.module.profile.dto.response.GuardianProfileResponse;
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
import com.tcs.module.profile.service.ClientLegalAccountService;
import com.tcs.module.profile.service.ClientLegalAccountService.LegalAccountContext;
import com.tcs.module.profile.service.ProfileService;
import com.tcs.module.profile.util.AgeUtils;
import com.tcs.module.profile.util.ChildProfileValidator;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final VerificationRequestRepository verificationRequestRepository;
    private final PlatformMapper platformMapper;
    private final ClientLegalAccountService clientLegalAccountService;
    private final ClassStudentRepository classStudentRepository;

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
        return parentChildLinkRepository
                .findByParentUser_UserIdAndStatus(ctx.user().getUserId(), ParentChildLinkStatus.ACTIVE)
                .stream()
                .map(link -> toChildResponse(link.getChildProfile()))
                .toList();
    }

    @Override
    @Transactional
    public ChildProfileResponse createChild(ChildProfileRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        requireAdultParent(ctx);
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu hồ sơ con là bắt buộc");
        }
        ChildProfile child = new ChildProfile();
        applyChildFields(child, request);
        ensureNoDuplicateChildForParent(
                ctx.user().getUserId(), child.getFullName(), child.getDateOfBirth(), null);
        ChildProfile saved = childProfileRepository.save(child);

        ParentChildLink link = new ParentChildLink();
        link.setParentUser(ctx.user());
        link.setChildProfile(saved);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        parentChildLinkRepository.save(link);
        return toChildResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ChildProfileResponse getChildById(Long childProfileId) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        ChildProfile child = requireLinkedChildProfile(ctx, ChildProfileValidator.requireChildProfileId(childProfileId));
        return toChildResponse(child);
    }

    @Override
    @Transactional
    public ChildProfileResponse updateChild(Long childProfileId, UpdateChildProfileRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        requireAdultParent(ctx);
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật là bắt buộc");
        }
        ChildProfile child =
                requireLinkedChildProfile(ctx, ChildProfileValidator.requireChildProfileId(childProfileId));
        boolean changed;
        if (findRegisteredMinorClient(child).isPresent()) {
            rejectIdentityFieldChangesForLinkedAccount(request);
            changed = applyLinkedChildSupplement(child, request);
        } else {
            changed = applyManualChildUpdate(child, request);
            if (changed) {
                validateManualChildProfile(child);
                ensureNoDuplicateChildForParent(
                        ctx.user().getUserId(), child.getFullName(), child.getDateOfBirth(), child.getChildProfileId());
            }
        }
        if (!changed) {
            throw new IllegalArgumentException("Không có thay đổi nào để cập nhật");
        }
        return toChildResponse(childProfileRepository.save(child));
    }

    @Override
    @Transactional
    public void deleteChild(Long childProfileId) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        requireAdultParent(ctx);
        Long validatedId = ChildProfileValidator.requireChildProfileId(childProfileId);
        ParentChildLink link = parentChildLinkRepository
                .findFirstByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
                        ctx.user().getUserId(), validatedId, ParentChildLinkStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ con"));
        ChildProfile child = link.getChildProfile();
        boolean linkedToUserAccount = isRegisteredAccountChild(child);
        parentChildLinkRepository.delete(link);
        parentChildLinkRepository.flush();
        if (!linkedToUserAccount
                && !parentChildLinkRepository.existsByChildProfile_ChildProfileIdAndStatus(
                        validatedId, ParentChildLinkStatus.ACTIVE)
                && !classStudentRepository.existsByChildProfile_ChildProfileId(validatedId)) {
            try {
                childProfileRepository.delete(child);
            } catch (DataIntegrityViolationException ex) {
                // Hồ sơ con còn được tham chiếu ở nơi khác; gỡ liên kết phụ huynh vẫn thành công.
            }
        }
    }

    private boolean isRegisteredAccountChild(ChildProfile child) {
        return resolveRegisteredMinorClient(child).isPresent();
    }

    private Optional<Client> resolveRegisteredMinorClient(ChildProfile child) {
        Optional<Client> direct = findRegisteredMinorClient(child);
        if (direct.isPresent()) {
            return direct;
        }
        Long profileId = child.getChildProfileId();
        LocalDate adultThreshold =
                LocalDate.now().minusYears(AgeUtils.ADULT_AGE_THRESHOLD);
        for (Client minorClient : clientRepository.findByDateOfBirthAfter(adultThreshold)) {
            if (minorClient.getDateOfBirth() == null || !AgeUtils.isMinor(minorClient.getDateOfBirth())) {
                continue;
            }
            Optional<ParentChildLink> guardianLink =
                    clientLegalAccountService.findGuardianLinkForMinor(minorClient);
            if (guardianLink.isPresent()
                    && guardianLink.get().getChildProfile().getChildProfileId().equals(profileId)) {
                return Optional.of(minorClient);
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public ChildProfileResponse linkChild(LinkChildRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        ChildProfile child = childProfileRepository
                .findById(request.getChildProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ con"));
        if (parentChildLinkRepository.existsByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
                ctx.user().getUserId(), child.getChildProfileId(), ParentChildLinkStatus.ACTIVE)) {
            throw new IllegalArgumentException("Hồ sơ con đã được liên kết");
        }
        ParentChildLink link = new ParentChildLink();
        link.setParentUser(ctx.user());
        link.setChildProfile(child);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        parentChildLinkRepository.save(link);
        return toChildResponse(child);
    }

    @Override
    @Transactional
    public ChildProfileResponse linkChildAccount(LinkChildAccountRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        requireAdultParent(ctx);
        if (!StringUtils.hasText(request.getChildEmail())) {
            throw new IllegalArgumentException("Email tài khoản con là bắt buộc");
        }
        User childUser = userRepository
                .findByEmail(request.getChildEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này"));
        if (childUser.getUserId().equals(ctx.user().getUserId())) {
            throw new IllegalArgumentException("Không thể liên kết chính tài khoản của bạn");
        }
        Client childClient = clientRepository
                .findByUser_UserId(childUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản này không phải khách hàng hợp lệ"));
        if (childClient.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Tài khoản con cần cập nhật ngày sinh trước khi liên kết");
        }
        if (!StringUtils.hasText(childClient.getFullName())) {
            throw new IllegalArgumentException("Tài khoản con cần cập nhật họ tên trước khi liên kết");
        }
        if (!AgeUtils.isMinor(childClient.getDateOfBirth())) {
            throw new IllegalArgumentException("Chỉ có thể liên kết tài khoản học sinh vị thành niên (dưới 18 tuổi)");
        }
        Optional<ParentChildLink> existingLink =
                clientLegalAccountService.findGuardianLinkForMinor(childClient);
        if (existingLink.isPresent()) {
            if (existingLink.get().getParentUser().getUserId().equals(ctx.user().getUserId())) {
                throw new IllegalArgumentException("Tài khoản con đã được liên kết");
            }
            throw new IllegalArgumentException("Học sinh đã liên kết với phụ huynh khác");
        }
        ChildProfile childProfile = resolveChildProfileForMinor(childClient);
        ParentChildLink link = new ParentChildLink();
        link.setParentUser(ctx.user());
        link.setChildProfile(childProfile);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        parentChildLinkRepository.save(link);
        return toChildResponse(childProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public DependentLinkStatusResponse getDependentLinkStatus() {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        Client client = requireClient(ctx);
        return buildDependentLinkStatus(client);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianProfileResponse getMyGuardian() {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        Client client = requireClient(ctx);
        if (!AgeUtils.isMinor(client.getDateOfBirth())) {
            throw new ForbiddenException("Chỉ tài khoản học sinh vị thành niên mới có phụ huynh liên kết");
        }
        return clientLegalAccountService.findGuardianLinkForMinor(client).map(this::toGuardianResponse).orElse(null);
    }

    @Override
    @Transactional
    public GuardianProfileResponse linkGuardian(LinkGuardianRequest request) {
        ProfileContext ctx = loadContext();
        requireRole(ctx, UserRole.CLIENT);
        Client client = requireClient(ctx);
        if (client.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Vui lòng cập nhật ngày sinh trước khi liên kết phụ huynh");
        }
        if (!StringUtils.hasText(client.getFullName())) {
            throw new IllegalArgumentException("Vui lòng cập nhật họ tên trước khi liên kết phụ huynh");
        }
        if (!AgeUtils.isMinor(client.getDateOfBirth())) {
            throw new ForbiddenException("Chỉ tài khoản học sinh vị thành niên cần liên kết phụ huynh");
        }
        if (!StringUtils.hasText(request.getParentEmail())) {
            throw new IllegalArgumentException("Email phụ huynh là bắt buộc");
        }
        User parentUser = userRepository
                .findByEmail(request.getParentEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản phụ huynh với email này"));
        if (parentUser.getUserId().equals(ctx.user().getUserId())) {
            throw new IllegalArgumentException("Không thể liên kết chính tài khoản của bạn");
        }
        Client parentClient = clientRepository
                .findByUser_UserId(parentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản phụ huynh không phải khách hàng hợp lệ"));
        if (parentClient.getDateOfBirth() == null || !AgeUtils.isAdult(parentClient.getDateOfBirth())) {
            throw new IllegalArgumentException("Phụ huynh liên kết phải từ 18 tuổi trở lên");
        }
        if (parentChildLinkRepository
                .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                        parentUser.getUserId(), client.getFullName(), client.getDateOfBirth(), ParentChildLinkStatus.ACTIVE)
                .isPresent()) {
            throw new IllegalArgumentException("Phụ huynh này đã được liên kết");
        }
        ChildProfile childProfile = resolveChildProfileForMinor(client);
        ParentChildLink link = new ParentChildLink();
        link.setParentUser(parentUser);
        link.setChildProfile(childProfile);
        link.setStatus(ParentChildLinkStatus.ACTIVE);
        return toGuardianResponse(parentChildLinkRepository.save(link));
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
    public void submitVerification() {
        ProfileContext ctx = loadContext();
        if (ctx.role() != UserRole.TUTOR && ctx.role() != UserRole.TUTOR_CENTER) {
            throw new ForbiddenException("Chỉ gia sư hoặc trung tâm mới nộp xác minh");
        }
        VerificationRequest request = new VerificationRequest();
        request.setUser(ctx.user());
        request.setVerificationType(
                ctx.role() == UserRole.TUTOR ? VerificationType.TUTOR_PROFILE : VerificationType.TUTOR_CENTER_LICENSE);
        request.setStatus(VerificationStatus.SUBMITTED);
        request.setSubmittedAt(LocalDateTime.now());
        verificationRequestRepository.save(request);
    }

    private Client requireClient(ProfileContext ctx) {
        if (ctx.client() == null) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng");
        }
        return ctx.client();
    }

    private DependentLinkStatusResponse buildDependentLinkStatus(Client client) {
        LocalDate dateOfBirth = client.getDateOfBirth();
        boolean dateOfBirthMissing = dateOfBirth == null;
        boolean minorAccount = !dateOfBirthMissing && AgeUtils.isMinor(dateOfBirth);
        boolean guardianRequired = minorAccount;
        boolean guardianLinked = minorAccount
                && StringUtils.hasText(client.getFullName())
                && clientLegalAccountService.findGuardianLinkForMinor(client).isPresent();
        int linkedChildrenCount = (int) parentChildLinkRepository
                .findByParentUser_UserIdAndStatus(client.getUser().getUserId(), ParentChildLinkStatus.ACTIVE)
                .size();
        boolean childrenLinkOptional = !dateOfBirthMissing && AgeUtils.isAdult(dateOfBirth);
        boolean profileLinkComplete =
                !dateOfBirthMissing && (!guardianRequired || guardianLinked);

        boolean legalProceduresDelegatedToParent = false;
        Long legalAccountUserId = null;
        String legalAccountHolderName = null;
        String legalAccountEmail = null;

        if (guardianLinked) {
            LegalAccountContext legalContext = clientLegalAccountService.resolveForClient(client);
            legalProceduresDelegatedToParent = legalContext.isDelegatedToParent();
            legalAccountUserId = legalContext.getLegalUserId();
            legalAccountHolderName = legalContext.getLegalHolderName();
            legalAccountEmail = legalContext.getLegalHolderEmail();
        }

        return DependentLinkStatusResponse.builder()
                .dateOfBirthMissing(dateOfBirthMissing)
                .minorAccount(minorAccount)
                .guardianRequired(guardianRequired)
                .guardianLinked(guardianLinked)
                .childrenLinkOptional(childrenLinkOptional)
                .linkedChildrenCount(linkedChildrenCount)
                .profileLinkComplete(profileLinkComplete)
                .canProceedToPayment(profileLinkComplete)
                .legalProceduresDelegatedToParent(legalProceduresDelegatedToParent)
                .parentApprovalRequired(legalProceduresDelegatedToParent)
                .legalAccountUserId(legalAccountUserId)
                .legalAccountHolderName(legalAccountHolderName)
                .legalAccountEmail(legalAccountEmail)
                .build();
    }

    private ChildProfile resolveChildProfileForMinor(Client client) {
        return childProfileRepository
                .findFirstByFullNameAndDateOfBirth(client.getFullName(), client.getDateOfBirth())
                .orElseGet(() -> {
                    ChildProfile child = new ChildProfile();
                    child.setFullName(client.getFullName());
                    child.setDateOfBirth(client.getDateOfBirth());
                    child.setGender(client.getGender());
                    return childProfileRepository.save(child);
                });
    }

    private GuardianProfileResponse toGuardianResponse(ParentChildLink link) {
        Client parentClient = clientRepository
                .findByUser_UserId(link.getParentUser().getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ phụ huynh"));
        return GuardianProfileResponse.builder()
                .parentUserId(link.getParentUser().getUserId())
                .fullName(parentClient.getFullName())
                .email(link.getParentUser().getEmail())
                .phone(parentClient.getPhone())
                .linkedAt(link.getCreatedAt())
                .build();
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

    private void requireAdultParent(ProfileContext ctx) {
        Client parentClient = requireClient(ctx);
        if (parentClient.getDateOfBirth() == null || !AgeUtils.isAdult(parentClient.getDateOfBirth())) {
            throw new ForbiddenException("Chỉ tài khoản phụ huynh từ 18 tuổi trở lên mới quản lý hồ sơ con");
        }
    }

    private void ensureNoDuplicateChildForParent(
            Long parentUserId, String fullName, LocalDate dateOfBirth, Long excludeChildProfileId) {
        if (!StringUtils.hasText(fullName) || dateOfBirth == null) {
            return;
        }
        parentChildLinkRepository
                .findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
                        parentUserId, fullName, dateOfBirth, ParentChildLinkStatus.ACTIVE)
                .ifPresent(link -> {
                    Long existingId = link.getChildProfile().getChildProfileId();
                    if (excludeChildProfileId == null || !excludeChildProfileId.equals(existingId)) {
                        throw new IllegalArgumentException(
                                "Đã tồn tại hồ sơ con với cùng họ tên và ngày sinh");
                    }
                });
    }

    private void validateManualChildProfile(ChildProfile child) {
        child.setFullName(ChildProfileValidator.requireFullName(child.getFullName()));
        ChildProfileValidator.validateChildDateOfBirth(child.getDateOfBirth(), false);
        ChildProfileValidator.validateGender(child.getGender());
    }

    private ChildProfile requireLinkedChildProfile(ProfileContext ctx, Long childProfileId) {
        parentChildLinkRepository
                .findFirstByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
                        ctx.user().getUserId(), childProfileId, ParentChildLinkStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Không có quyền truy cập hồ sơ con này"));
        return childProfileRepository
                .findById(childProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ con"));
    }

    private void rejectIdentityFieldChangesForLinkedAccount(UpdateChildProfileRequest request) {
        if (StringUtils.hasText(request.getFullName())
                || request.getDateOfBirth() != null
                || request.getGender() != null) {
            throw new ForbiddenException(
                    "Hồ sơ liên kết tài khoản đăng ký: phụ huynh chỉ cập nhật khối/lớp, trường học và ghi chú");
        }
    }

    private boolean applyLinkedChildSupplement(ChildProfile child, UpdateChildProfileRequest request) {
        boolean changed = false;
        if (request.getSchoolName() != null) {
            String normalized = ChildProfileValidator.normalizeSchoolName(request.getSchoolName());
            if (!Objects.equals(normalized, normalizeStoredText(child.getSchoolName()))) {
                child.setSchoolName(normalized);
                changed = true;
            }
        }
        if (request.getNotes() != null) {
            String normalized = ChildProfileValidator.normalizeNotes(request.getNotes());
            if (!Objects.equals(normalized, normalizeStoredText(child.getNotes()))) {
                child.setNotes(normalized);
                changed = true;
            }
        }
        if (request.getGradeId() != null) {
            ChildProfileValidator.validateGradeId(request.getGradeId());
            changed |= applyGradeIfChanged(child, request.getGradeId());
        }
        return changed;
    }

    private boolean applyManualChildUpdate(ChildProfile child, UpdateChildProfileRequest request) {
        boolean changed = false;
        if (request.getFullName() != null) {
            String normalized = ChildProfileValidator.requireFullName(request.getFullName());
            if (!Objects.equals(normalized, child.getFullName())) {
                child.setFullName(normalized);
                changed = true;
            }
        }
        if (request.getDateOfBirth() != null) {
            LocalDate validated = ChildProfileValidator.validateChildDateOfBirth(request.getDateOfBirth(), false);
            if (!Objects.equals(validated, child.getDateOfBirth())) {
                child.setDateOfBirth(validated);
                changed = true;
            }
        }
        if (request.getGender() != null) {
            ChildProfileValidator.validateGender(request.getGender());
            if (request.getGender() != child.getGender()) {
                child.setGender(request.getGender());
                changed = true;
            }
        }
        if (request.getSchoolName() != null) {
            String normalized = ChildProfileValidator.normalizeSchoolName(request.getSchoolName());
            if (!Objects.equals(normalized, normalizeStoredText(child.getSchoolName()))) {
                child.setSchoolName(normalized);
                changed = true;
            }
        }
        if (request.getNotes() != null) {
            String normalized = ChildProfileValidator.normalizeNotes(request.getNotes());
            if (!Objects.equals(normalized, normalizeStoredText(child.getNotes()))) {
                child.setNotes(normalized);
                changed = true;
            }
        }
        if (request.getGradeId() != null) {
            ChildProfileValidator.validateGradeId(request.getGradeId());
            changed |= applyGradeIfChanged(child, request.getGradeId());
        }
        return changed;
    }

    private String normalizeStoredText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean applyGradeIfChanged(ChildProfile child, Long gradeId) {
        Long currentGradeId = child.getGrade() != null ? child.getGrade().getGradeId() : null;
        if (gradeId == 0) {
            if (currentGradeId == null) {
                return false;
            }
            child.setGrade(null);
            return true;
        }
        if (Objects.equals(gradeId, currentGradeId)) {
            return false;
        }
        Grade grade = gradeRepository
                .findById(gradeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối/lớp"));
        child.setGrade(grade);
        return true;
    }

    private void applyGrade(ChildProfile child, Long gradeId) {
        if (gradeId == null) {
            return;
        }
        applyGradeIfChanged(child, gradeId);
    }

    private void applyChildFields(ChildProfile child, ChildProfileRequest request) {
        child.setFullName(ChildProfileValidator.requireFullName(request.getFullName()));
        child.setDateOfBirth(ChildProfileValidator.validateChildDateOfBirth(request.getDateOfBirth(), false));
        ChildProfileValidator.validateGender(request.getGender());
        child.setGender(request.getGender());
        child.setSchoolName(ChildProfileValidator.normalizeSchoolName(request.getSchoolName()));
        child.setNotes(ChildProfileValidator.normalizeNotes(request.getNotes()));
        ChildProfileValidator.validateGradeId(request.getGradeId());
        applyGrade(child, request.getGradeId());
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
        ChildProfileResponse.ChildProfileResponseBuilder builder = ChildProfileResponse.builder()
                .childProfileId(child.getChildProfileId())
                .fullName(child.getFullName())
                .dateOfBirth(child.getDateOfBirth())
                .gender(child.getGender())
                .gradeId(child.getGrade() != null ? child.getGrade().getGradeId() : null)
                .gradeName(child.getGrade() != null ? child.getGrade().getGradeName() : null)
                .schoolName(child.getSchoolName())
                .notes(child.getNotes())
                .createdAt(child.getCreatedAt());
        resolveRegisteredMinorClient(child).ifPresent(childClient -> {
            builder.linkedToUserAccount(true)
                    .childUserId(childClient.getUser().getUserId())
                    .childEmail(childClient.getUser().getEmail())
                    .fullName(childClient.getFullName())
                    .dateOfBirth(childClient.getDateOfBirth())
                    .gender(childClient.getGender());
        });
        return builder.build();
    }

    private Optional<Client> findRegisteredMinorClient(ChildProfile child) {
        if (!StringUtils.hasText(child.getFullName()) || child.getDateOfBirth() == null) {
            return Optional.empty();
        }
        return clientRepository
                .findFirstByFullNameAndDateOfBirth(child.getFullName(), child.getDateOfBirth())
                .filter(client -> client.getDateOfBirth() != null && AgeUtils.isMinor(client.getDateOfBirth()));
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
