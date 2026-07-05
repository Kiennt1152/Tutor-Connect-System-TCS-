package com.tcs.module.platform.service.impl;

import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.enums.UserStatus;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.platform.dto.request.UpdateUserStatusRequest;
import com.tcs.module.platform.dto.response.PageUserListResponse;
import com.tcs.module.platform.dto.response.UserListItemResponse;
import com.tcs.module.platform.mapper.PlatformMapper;
import com.tcs.module.platform.mapper.UserProfileBundle;
import com.tcs.module.platform.service.PlatformService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.PlatformAdmin;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.entity.TutorCenter;
import com.tcs.module.identity.entity.VerificationDocument;
import com.tcs.module.identity.entity.VerificationHistory;
import com.tcs.module.identity.entity.VerificationRequest;
import com.tcs.module.identity.enums.VerificationStatus;
import com.tcs.module.identity.enums.VerificationType;
import com.tcs.module.identity.repository.VerificationDocumentRepository;
import com.tcs.module.identity.repository.VerificationHistoryRepository;
import com.tcs.module.identity.repository.VerificationRequestRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.dto.request.ReviewVerificationRequest;
import com.tcs.module.platform.dto.response.DashboardResponse;
import com.tcs.module.platform.dto.response.ReportResponse;
import com.tcs.module.platform.dto.response.VerificationDetailResponse;
import com.tcs.module.platform.dto.response.VerificationDocumentResponse;
import com.tcs.module.platform.dto.response.VerificationRequestResponse;
import com.tcs.module.profile.entity.MediaFile;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private static final int MAX_PAGE_SIZE = 50;
    /** BR-03: ly do tu choi toi thieu 10 ky tu. */
    private static final int MIN_REJECT_NOTES_LENGTH = 10;

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final TutorRepository tutorRepository;
    private final TutorCenterRepository tutorCenterRepository;
    private final ClientRepository clientRepository;
    private final PlatformMapper platformMapper;
    private final VerificationRequestRepository verificationRequestRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationHistoryRepository verificationHistoryRepository;
    private final ReportRepository reportRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final AuthHelper authHelper;

    @Override
    @Transactional(readOnly = true)
    public PageUserListResponse getUsers(
            int page, int size, UserStatus status, UserRole role, String keyword) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "userId"));

        Page<User> users = queryUsers(status, role, keyword, pageable);
        List<Long> userIds = users.getContent().stream().map(User::getUserId).toList();
        ProfileMaps profileMaps = loadProfileMaps(userIds);

        List<UserListItemResponse> content = users.getContent().stream()
                .map(user -> platformMapper.toUserListItem(user, profileMaps.bundleFor(user.getUserId())))
                .sorted(Comparator.comparing(UserListItemResponse::getUserId))
                .toList();

        return PageUserListResponse.builder()
                .content(content)
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public UserListItemResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        User user = findUserOrThrow(userId);
        UserProfileBundle profiles = loadProfiles(userId);

        if (platformMapper.resolveRole(profiles) == UserRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái tài khoản quản trị viên");
        }

        user.setStatus(request.getStatus());
        User saved = userRepository.save(user);
        return platformMapper.toUserListItem(saved, profiles);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long pendingVerifications = verificationRequestRepository.findAll().stream()
                .filter(v -> v.getStatus() == VerificationStatus.SUBMITTED
                        || v.getStatus() == VerificationStatus.UNDER_REVIEW)
                .count();
        long openReports = reportRepository.findAll().stream()
                .filter(r -> r.getStatus() == com.tcs.module.platform.enums.ReportStatus.PENDING)
                .count();
        return DashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalTutors(tutorRepository.count())
                .totalClasses(tutoringClassRepository.count())
                .pendingVerifications(pendingVerifications)
                .openReports(openReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> listVerificationRequests() {
        return verificationRequestRepository.findAll().stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public VerificationDetailResponse getVerificationDetail(Long verificationId) {
        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        // BR-01: mo mot ho so dang SUBMITTED se tu dong chuyen sang UNDER_REVIEW va ghi lich su.
        if (verification.getStatus() == VerificationStatus.SUBMITTED) {
            VerificationStatus oldStatus = verification.getStatus();
            verification.setStatus(VerificationStatus.UNDER_REVIEW);
            verification = verificationRequestRepository.save(verification);
            logHistory(verification, oldStatus, VerificationStatus.UNDER_REVIEW, currentAdminUser());
        }

        return buildDetail(verification);
    }

    @Override
    @Transactional
    public VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request) {
        VerificationRequest verification = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        VerificationStatus decision = request.getStatus();
        // Decision chi duoc la VERIFIED (Duyet) hoac REJECTED (Tu choi).
        if (decision != VerificationStatus.VERIFIED && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ. Chỉ chấp nhận Duyệt hoặc Từ chối.");
        }

        // BR-02 / AF-02: chi ho so dang UNDER_REVIEW moi duoc Duyet hoac Tu choi.
        if (verification.getStatus() != VerificationStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Hồ sơ này đã được xử lý bởi quản trị viên khác.");
        }

        // BR-03 / AF-01: khi Tu choi bat buoc nhap ly do (>= 10 ky tu). Khi Duyet thi bo qua ghi chu.
        String adminNotes = null;
        if (decision == VerificationStatus.REJECTED) {
            String trimmed = request.getAdminNotes() == null ? "" : request.getAdminNotes().trim();
            if (trimmed.length() < MIN_REJECT_NOTES_LENGTH) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).");
            }
            adminNotes = trimmed;
        }

        User admin = currentAdminUser();
        VerificationStatus oldStatus = verification.getStatus();

        verification.setStatus(decision);
        verification.setAdminNotes(adminNotes);
        verification.setReviewedAt(LocalDateTime.now());
        VerificationRequest saved = verificationRequestRepository.save(verification);

        // BR-06: moi lan chuyen trang thai deu ghi mot dong vao verification_histories.
        logHistory(saved, oldStatus, decision, admin);

        // BR-04: cap nhat verification_status cua nguoi nop theo dung loai ho so.
        ProfileVerificationStatus profileStatus = decision == VerificationStatus.VERIFIED
                ? ProfileVerificationStatus.VERIFIED
                : ProfileVerificationStatus.REJECTED;
        Long userId = saved.getUser().getUserId();
        if (saved.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            tutorRepository.findByUser_UserId(userId).ifPresent(tutor -> {
                tutor.setVerificationStatus(profileStatus);
                tutorRepository.save(tutor);
            });
        } else {
            tutorCenterRepository.findByUser_UserId(userId).ifPresent(center -> {
                center.setVerificationStatus(profileStatus);
                tutorCenterRepository.save(center);
            });
        }
        return toVerificationResponse(saved);
    }

    private VerificationDetailResponse buildDetail(VerificationRequest v) {
        Long userId = v.getUser().getUserId();
        Map<String, String> details = new LinkedHashMap<>();
        String submitterName = null;
        String submitterPhone = v.getUser().getPhone();

        if (v.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
            if (tutor != null) {
                submitterName = tutor.getFullName();
                if (StringUtils.hasText(tutor.getPhone())) {
                    submitterPhone = tutor.getPhone();
                }
                details.put("Giới tính", tutor.getGender() == null ? "—" : tutor.getGender().name());
                details.put("Số năm kinh nghiệm", String.valueOf(tutor.getExperienceYears()));
                details.put("Địa chỉ", orDash(tutor.getAddress()));
                details.put("Giới thiệu", orDash(tutor.getBio()));
                details.put("Trạng thái xác minh", tutor.getVerificationStatus().name());
            }
        } else {
            TutorCenter center = tutorCenterRepository.findByUser_UserId(userId).orElse(null);
            if (center != null) {
                submitterName = center.getCompanyName();
                if (StringUtils.hasText(center.getPhone())) {
                    submitterPhone = center.getPhone();
                }
                details.put("Số giấy phép", orDash(center.getLicenseNo()));
                details.put("Địa chỉ", orDash(center.getAddress()));
                details.put("Mô tả", orDash(center.getDescription()));
                details.put("Trạng thái xác minh", center.getVerificationStatus().name());
            }
        }

        List<VerificationDocumentResponse> documents = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(v.getVerificationId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        boolean hasUnreadable = documents.stream().anyMatch(doc -> !doc.isAvailable());

        return VerificationDetailResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(userId)
                .userEmail(v.getUser().getEmail())
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .submitterName(submitterName)
                .submitterPhone(submitterPhone)
                .submitterDetails(details)
                .documents(documents)
                .hasUnreadableDocument(hasUnreadable)
                .build();
    }

    private VerificationDocumentResponse toDocumentResponse(VerificationDocument doc) {
        MediaFile file = doc.getFile();
        boolean available = file != null && StringUtils.hasText(file.getFileUrl());
        return VerificationDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .documentType(doc.getDocumentType())
                .fileId(file == null ? null : file.getFileId())
                .fileName(file == null ? null : file.getFileName())
                .fileUrl(file == null ? null : file.getFileUrl())
                .mimeType(file == null ? null : file.getMimeType())
                .available(available)
                .build();
    }

    private void logHistory(
            VerificationRequest verification,
            VerificationStatus oldStatus,
            VerificationStatus newStatus,
            User admin) {
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(verification);
        history.setOldStatus(oldStatus == null ? null : oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(admin);
        verificationHistoryRepository.save(history);
    }

    private User currentAdminUser() {
        return userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên"));
    }

    private String orDash(String value) {
        return StringUtils.hasText(value) ? value : "—";
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listReports() {
        return reportRepository.findAll().stream().map(this::toReportResponse).toList();
    }

    private VerificationRequestResponse toVerificationResponse(VerificationRequest v) {
        return VerificationRequestResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(v.getUser().getUserId())
                .userEmail(v.getUser().getEmail())
                .verificationType(v.getVerificationType())
                .status(v.getStatus())
                .adminNotes(v.getAdminNotes())
                .submittedAt(v.getSubmittedAt())
                .reviewedAt(v.getReviewedAt())
                .build();
    }

    private ReportResponse toReportResponse(Report report) {
        return ReportResponse.builder()
                .reportId(report.getReportId())
                .reporterId(report.getReporter().getUserId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .category(report.getCategory())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private Page<User> queryUsers(UserStatus status, UserRole role, String keyword, PageRequest pageable) {
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        if (role != null && role != UserRole.UNKNOWN) {
            List<Long> roleUserIds = findUserIdsByRole(role);
            if (roleUserIds.isEmpty()) {
                return Page.empty(pageable);
            }
            return userRepository.searchUsersByIds(roleUserIds, status, trimmedKeyword, pageable);
        }

        return userRepository.searchUsers(status, trimmedKeyword, pageable);
    }

    private List<Long> findUserIdsByRole(UserRole role) {
        return switch (role) {
            case PLATFORM_ADMIN -> platformAdminRepository.findAll().stream()
                    .map(admin -> admin.getUser().getUserId())
                    .toList();
            case TUTOR -> tutorRepository.findAll().stream()
                    .map(tutor -> tutor.getUser().getUserId())
                    .toList();
            case TUTOR_CENTER -> tutorCenterRepository.findAll().stream()
                    .map(center -> center.getUser().getUserId())
                    .toList();
            case CLIENT -> clientRepository.findAll().stream()
                    .map(client -> client.getUser().getUserId())
                    .toList();
            default -> List.of();
        };
    }

    private User findUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private UserProfileBundle loadProfiles(Long userId) {
        return UserProfileBundle.of(
                platformAdminRepository.findByUser_UserId(userId).orElse(null),
                tutorRepository.findByUser_UserId(userId).orElse(null),
                tutorCenterRepository.findByUser_UserId(userId).orElse(null),
                clientRepository.findByUser_UserId(userId).orElse(null));
    }

    private ProfileMaps loadProfileMaps(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return ProfileMaps.empty();
        }

        Map<Long, PlatformAdmin> adminMap = toMapByUserId(
                platformAdminRepository.findByUser_UserIdIn(userIds), PlatformAdmin::getUser);
        Map<Long, Tutor> tutorMap =
                toMapByUserId(tutorRepository.findByUser_UserIdIn(userIds), Tutor::getUser);
        Map<Long, TutorCenter> centerMap = toMapByUserId(
                tutorCenterRepository.findByUser_UserIdIn(userIds), TutorCenter::getUser);
        Map<Long, Client> clientMap =
                toMapByUserId(clientRepository.findByUser_UserIdIn(userIds), Client::getUser);

        return new ProfileMaps(adminMap, tutorMap, centerMap, clientMap);
    }

    private <T> Map<Long, T> toMapByUserId(List<T> items, Function<T, User> userExtractor) {
        return items.stream()
                .collect(Collectors.toMap(item -> userExtractor.apply(item).getUserId(), Function.identity()));
    }

    private record ProfileMaps(
            Map<Long, PlatformAdmin> admins,
            Map<Long, Tutor> tutors,
            Map<Long, TutorCenter> centers,
            Map<Long, Client> clients) {

        static ProfileMaps empty() {
            return new ProfileMaps(Map.of(), Map.of(), Map.of(), Map.of());
        }

        UserProfileBundle bundleFor(Long userId) {
            return UserProfileBundle.of(
                    admins.get(userId), tutors.get(userId), centers.get(userId), clients.get(userId));
        }
    }
}
