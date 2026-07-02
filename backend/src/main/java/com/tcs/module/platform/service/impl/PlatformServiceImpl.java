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
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.repository.ReportRepository;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.PlatformAdminRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlatformServiceImpl implements PlatformService {

    private static final int MAX_PAGE_SIZE = 50;

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
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }

        User user = findUserOrThrow(userId);
        UserProfileBundle profiles = loadProfiles(userId);

        if (platformMapper.resolveRole(profiles) == UserRole.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Không thể thay đổi trạng thái tài khoản quản trị viên");
        }

        UserStatus newStatus = request.getStatus();
        if (newStatus != UserStatus.ACTIVE
                && newStatus != UserStatus.SUSPENDED
                && newStatus != UserStatus.BANNED) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }

        user.setStatus(newStatus);
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
        // Chi hien thi ban ghi dang cho xu ly: SUBMITTED / UNDER_REVIEW (DRAFT khong hien).
        return verificationRequestRepository
                .findByStatusInOrderBySubmittedAtAsc(
                        List.of(VerificationStatus.SUBMITTED, VerificationStatus.UNDER_REVIEW))
                .stream()
                .map(this::toVerificationResponse)
                .toList();
    }

    @Override
    @Transactional
    public VerificationDetailResponse openVerification(Long verificationId) {
        VerificationRequest req = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        // BR-01: mo ban ghi SUBMITTED -> chuyen UNDER_REVIEW va ghi lich su.
        if (req.getStatus() == VerificationStatus.SUBMITTED) {
            VerificationStatus oldStatus = req.getStatus();
            req.setStatus(VerificationStatus.UNDER_REVIEW);
            req = verificationRequestRepository.save(req);
            logHistory(req, oldStatus, VerificationStatus.UNDER_REVIEW);
        }
        return buildDetail(req);
    }

    @Override
    @Transactional
    public VerificationRequestResponse reviewVerification(Long verificationId, ReviewVerificationRequest request) {
        VerificationStatus decision = request.getStatus();
        if (decision != VerificationStatus.VERIFIED && decision != VerificationStatus.REJECTED) {
            throw new IllegalArgumentException("Quyết định không hợp lệ (chỉ Duyệt hoặc Từ chối)");
        }

        VerificationRequest req = verificationRequestRepository
                .findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu xác minh"));

        // BR-02 / AF-02: chi ban ghi UNDER_REVIEW moi duoc duyet/tu choi.
        if (req.getStatus() != VerificationStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Yêu cầu này đã được xử lý.");
        }

        // BR-03 / AF-01: tu choi bat buoc co ly do (toi thieu 10 ky tu, khong tinh khoang trang).
        String notes = request.getAdminNotes() == null ? "" : request.getAdminNotes().trim();
        if (decision == VerificationStatus.REJECTED && notes.length() < 10) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối (tối thiểu 10 ký tự).");
        }

        VerificationStatus oldStatus = req.getStatus();
        req.setStatus(decision);
        req.setReviewedAt(LocalDateTime.now());
        req.setAdminNotes(decision == VerificationStatus.REJECTED ? notes : null);
        VerificationRequest saved = verificationRequestRepository.save(req);

        // BR-06: ghi lich su chuyen trang thai.
        logHistory(saved, oldStatus, decision);

        // BR-04: cap nhat verification_status cua ho so nguoi nop.
        ProfileVerificationStatus profileStatus = decision == VerificationStatus.VERIFIED
                ? ProfileVerificationStatus.VERIFIED
                : ProfileVerificationStatus.REJECTED;
        Long userId = saved.getUser().getUserId();
        tutorRepository.findByUser_UserId(userId).ifPresent(tutor -> {
            tutor.setVerificationStatus(profileStatus);
            tutorRepository.save(tutor);
        });
        tutorCenterRepository.findByUser_UserId(userId).ifPresent(center -> {
            center.setVerificationStatus(profileStatus);
            tutorCenterRepository.save(center);
        });

        return toVerificationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> listReports() {
        return reportRepository.findAll().stream().map(this::toReportResponse).toList();
    }

    /** BR-06: ghi mot dong lich su cho moi lan chuyen trang thai. */
    private void logHistory(VerificationRequest req, VerificationStatus oldStatus, VerificationStatus newStatus) {
        User admin = userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quản trị viên"));
        VerificationHistory history = new VerificationHistory();
        history.setVerificationRequest(req);
        history.setOldStatus(oldStatus == null ? null : oldStatus.name());
        history.setNewStatus(newStatus.name());
        history.setChangedByUser(admin);
        verificationHistoryRepository.save(history);
    }

    private VerificationDetailResponse buildDetail(VerificationRequest req) {
        Long userId = req.getUser().getUserId();
        Map<String, String> details = new LinkedHashMap<>();
        String submitterName = null;
        String submitterPhone = null;

        if (req.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            Tutor tutor = tutorRepository.findByUser_UserId(userId).orElse(null);
            if (tutor != null) {
                submitterName = tutor.getFullName();
                submitterPhone = tutor.getPhone();
                details.put("Giới tính", tutor.getGender() == null ? "-" : tutor.getGender().name());
                details.put("Số năm kinh nghiệm", String.valueOf(tutor.getExperienceYears()));
                if (StringUtils.hasText(tutor.getAddress())) {
                    details.put("Địa chỉ", tutor.getAddress());
                }
                if (StringUtils.hasText(tutor.getBio())) {
                    details.put("Giới thiệu", tutor.getBio());
                }
                details.put("Trạng thái xác minh hiện tại", tutor.getVerificationStatus().name());
            }
        } else {
            TutorCenter center = tutorCenterRepository.findByUser_UserId(userId).orElse(null);
            if (center != null) {
                submitterName = center.getCompanyName();
                submitterPhone = center.getPhone();
                if (StringUtils.hasText(center.getLicenseNo())) {
                    details.put("Số giấy phép", center.getLicenseNo());
                }
                if (StringUtils.hasText(center.getAddress())) {
                    details.put("Địa chỉ", center.getAddress());
                }
                details.put("Trạng thái xác minh hiện tại", center.getVerificationStatus().name());
            }
        }

        List<VerificationDocumentResponse> documents = verificationDocumentRepository
                .findByVerificationRequest_VerificationId(req.getVerificationId())
                .stream()
                .map(this::toDocumentResponse)
                .toList();
        boolean hasUnreadable = documents.stream().anyMatch(d -> !d.isAvailable());

        return VerificationDetailResponse.builder()
                .verificationId(req.getVerificationId())
                .userId(userId)
                .userEmail(req.getUser().getEmail())
                .verificationType(req.getVerificationType())
                .status(req.getStatus())
                .adminNotes(req.getAdminNotes())
                .submittedAt(req.getSubmittedAt())
                .reviewedAt(req.getReviewedAt())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .submitterName(submitterName)
                .submitterPhone(submitterPhone)
                .submitterDetails(details)
                .documents(documents)
                .hasUnreadableDocument(hasUnreadable)
                .build();
    }

    private VerificationDocumentResponse toDocumentResponse(VerificationDocument doc) {
        var file = doc.getFile();
        boolean available = file != null && StringUtils.hasText(file.getFileUrl());
        return VerificationDocumentResponse.builder()
                .documentId(doc.getDocumentId())
                .documentType(doc.getDocumentType())
                .fileId(file != null ? file.getFileId() : null)
                .fileName(file != null ? file.getFileName() : null)
                .fileUrl(file != null ? file.getFileUrl() : null)
                .mimeType(file != null ? file.getMimeType() : null)
                .available(available)
                .build();
    }

    private String resolveSubmitterName(VerificationRequest v) {
        Long userId = v.getUser().getUserId();
        if (v.getVerificationType() == VerificationType.TUTOR_PROFILE) {
            return tutorRepository.findByUser_UserId(userId).map(Tutor::getFullName).orElse(null);
        }
        return tutorCenterRepository.findByUser_UserId(userId).map(TutorCenter::getCompanyName).orElse(null);
    }

    private VerificationRequestResponse toVerificationResponse(VerificationRequest v) {
        return VerificationRequestResponse.builder()
                .verificationId(v.getVerificationId())
                .userId(v.getUser().getUserId())
                .userEmail(v.getUser().getEmail())
                .submitterName(resolveSubmitterName(v))
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
            return filterUsersByIds(roleUserIds, status, trimmedKeyword, pageable);
        }

        if (status != null && trimmedKeyword != null) {
            return userRepository.findByStatusAndEmailContainingIgnoreCase(status, trimmedKeyword, pageable);
        }
        if (status != null) {
            return userRepository.findByStatus(status, pageable);
        }
        if (trimmedKeyword != null) {
            return userRepository.findByEmailContainingIgnoreCase(trimmedKeyword, pageable);
        }
        return userRepository.findAll(pageable);
    }

    private Page<User> filterUsersByIds(
            List<Long> userIds, UserStatus status, String keyword, PageRequest pageable) {
        Page<User> page = userRepository.findByUserIdIn(userIds, pageable);
        if (status == null && keyword == null) {
            return page;
        }

        List<User> filtered = page.getContent().stream()
                .filter(user -> status == null || user.getStatus() == status)
                .filter(user -> keyword == null
                        || user.getEmail().toLowerCase().contains(keyword.toLowerCase()))
                .sorted(Comparator.comparing(User::getUserId))
                .toList();

        if (filtered.size() == page.getContent().size()) {
            return page;
        }

        return new PageImpl<>(filtered, pageable, filtered.size());
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
