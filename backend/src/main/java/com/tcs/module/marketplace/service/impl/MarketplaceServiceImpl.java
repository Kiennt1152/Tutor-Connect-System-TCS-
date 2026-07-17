package com.tcs.module.marketplace.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.identity.repository.UserRepository;
import com.tcs.module.marketplace.dto.request.ApplyClassRequest;
import com.tcs.module.marketplace.dto.request.CreateClassRequest;
import com.tcs.module.marketplace.dto.response.ApplicantResponse;
import com.tcs.module.marketplace.dto.response.ClassResponse;
import com.tcs.module.marketplace.dto.response.TutorSearchResponse;
import com.tcs.module.marketplace.entity.FavoriteTutor;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.FavoriteTutorRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.marketplace.service.MarketplaceService;
import com.tcs.module.profile.entity.Client;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.ClientRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final FavoriteTutorRepository favoriteTutorRepository;
    private final CategoryRepository categoryRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocationRepository locationRepository;
    // Khởi tạo trực tiếp (không có bean ObjectMapper trong context) — giống GoogleTokenVerifier.
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Học phí/giờ thấp nhất chấp nhận được (đ) — khớp FEE_PER_HOUR_MIN của form phía client. */
    private static final BigDecimal MIN_RATE_PER_HOUR = BigDecimal.valueOf(50_000);

    /** Khóa của môn "Khác" (tự nhập) trong detailsJson.subjectIds. */
    private static final String OTHER_SUBJECT_KEY = "other";

    /** Giới hạn cột tutoring_classes.title. */
    private static final int TITLE_MAX_LENGTH = 150;

    /** Khối phổ thông dạng "Lớp 12" — bắt lấy phần số để ghép " lớp 12". */
    private static final Pattern GRADE_NUMBER_PATTERN =
            Pattern.compile("^Lớp\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> listClasses(TutoringClassStatus status) {
        List<TutoringClass> classes =
                status != null ? tutoringClassRepository.findByStatus(status) : tutoringClassRepository.findAll();
        return classes.stream().map(this::toClassResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClass(Long classId) {
        return toClassResponse(findClass(classId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> listMyClasses() {
        return tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId()).stream()
                .map(this::toClassResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        User creator = requireUser();
        requireClient(creator.getUserId());
        if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
            throw new IllegalArgumentException("Vui lòng chọn môn học");
        }
        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(creator);
        applyRequest(tutoringClass, request);
        tutoringClass.setBudget(request.getBudget() != null ? request.getBudget() : BigDecimal.ZERO);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long classId, CreateClassRequest request) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền sửa lớp này");
        }
        if (tutoringClass.getStatus() != TutoringClassStatus.DRAFT) {
            throw new IllegalArgumentException("Lớp đã đăng thì không thể sửa nữa");
        }
        if (request.getSubjectId() == null && !StringUtils.hasText(request.getDetailsJson())) {
            throw new IllegalArgumentException("Vui lòng chọn môn học");
        }
        applyRequest(tutoringClass, request);
        if (request.getBudget() != null) tutoringClass.setBudget(request.getBudget());
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    /** Áp các trường từ request vào entity; tự sinh tiêu đề/mô tả khi bỏ trống. */
    private void applyRequest(TutoringClass tutoringClass, CreateClassRequest request) {
        Subject subject = resolveSubject(request.getSubjectId());
        Grade grade = resolveGrade(request.getGradeId());
        tutoringClass.setCategory(resolveCategory(request.getCategoryId()));
        tutoringClass.setSubject(subject);
        tutoringClass.setGrade(grade);
        tutoringClass.setLearningGoal(trimToNull(request.getLearningGoal()));
        tutoringClass.setTutorRequirement(trimToNull(request.getTutorRequirement()));
        tutoringClass.setLocation(resolveLocation(request.getLocationId()));
        tutoringClass.setAddress(trimToNull(request.getAddress()));
        tutoringClass.setTitle(resolveTitle(request, subject, grade));
        tutoringClass.setDescription(resolveDescription(request, subject, grade));
        tutoringClass.setDetailsJson(request.getDetailsJson());
        if (request.getLessonMode() != null) tutoringClass.setLessonMode(request.getLessonMode());
        if (request.getNumberOfSessions() != null) tutoringClass.setNumberOfSessions(request.getNumberOfSessions());
        if (request.getStartDate() != null) tutoringClass.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) tutoringClass.setEndDate(request.getEndDate());
        if (request.getTuitionFee() != null) tutoringClass.setTuitionFee(request.getTuitionFee());
        if (request.getRecurringType() != null) tutoringClass.setRecurringType(request.getRecurringType());
    }

    /**
     * Tiêu đề tự sinh nêu ĐỦ các môn của lớp (lấy từ detailsJson), không chỉ môn chính —
     * lớp Toán + Lý + Hóa phải hiện cả 3 để gia sư biết mình có dạy đủ hay không.
     */
    private String resolveTitle(CreateClassRequest request, Subject subject, Grade grade) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle().trim();
        }
        return autoTitle(request.getDetailsJson(), subject, grade);
    }

    /** Tiêu đề tự sinh, bỏ qua tiêu đề người dùng nhập (dùng cả cho mô tả mặc định). */
    private String autoTitle(String detailsJson, Subject subject, Grade grade) {
        List<String> names = subjectNamesFromJson(detailsJson);
        if (names.isEmpty() && subject != null) {
            names = List.of(subject.getSubjectName());
        }
        StringBuilder sb = new StringBuilder("Cần tìm gia sư");
        if (!names.isEmpty()) {
            sb.append(names.size() > 1 ? " các môn " : " môn ").append(String.join(", ", names));
        }
        if (grade != null) {
            sb.append(gradeSuffix(grade.getGradeName()));
        }
        String title = sb.toString();
        return title.length() > TITLE_MAX_LENGTH
                ? title.substring(0, TITLE_MAX_LENGTH - 1) + "…"
                : title;
    }

    private String resolveDescription(CreateClassRequest request, Subject subject, Grade grade) {
        if (StringUtils.hasText(request.getDescription())) {
            return request.getDescription().trim();
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.getLearningGoal())) {
            sb.append("Mục tiêu: ").append(request.getLearningGoal().trim());
        }
        if (StringUtils.hasText(request.getTutorRequirement())) {
            if (sb.length() > 0) sb.append(". ");
            sb.append("Yêu cầu gia sư: ").append(request.getTutorRequirement().trim());
        }
        if (sb.length() == 0) {
            sb.append(autoTitle(request.getDetailsJson(), subject, grade));
        }
        return sb.toString();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    @Override
    @Transactional
    public ClassResponse publishClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền đăng lớp này");
        }
        tutoringClass.setStatus(TutoringClassStatus.OPEN);
        return toClassResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public void applyToClass(Long classId, ApplyClassRequest request) {
        Tutor tutor = requireTutor();
        TutoringClass tutoringClass = findClass(classId);
        if (tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp không mở đơn ứng tuyển");
        }
        Map<String, BigDecimal> rates = resolveProposedRates(request, tutoringClass);

        TutorApplication application = new TutorApplication();
        application.setTutoringClass(tutoringClass);
        application.setTutor(tutor);
        application.setProposedRatesJson(rates.isEmpty() ? null : writeJson(rates));
        application.setProposedRate(highestRate(rates, request.getProposedRate()));
        application.setCoverLetter(request.getCoverLetter());
        application.setStatus(TutorApplicationStatus.SUBMITTED);
        tutorApplicationRepository.save(application);
    }

    /**
     * Gia sư phải báo giá cho MỌI môn của lớp. Đơn gửi từ client cũ (chỉ có một mức chung)
     * được quy về mức đó cho từng môn để dữ liệu đồng nhất.
     */
    private Map<String, BigDecimal> resolveProposedRates(
            ApplyClassRequest request, TutoringClass tutoringClass) {
        List<String> subjectKeys = classSubjectKeys(tutoringClass);
        Map<String, BigDecimal> requested = request.getProposedRates();
        Map<String, BigDecimal> resolved = new LinkedHashMap<>();

        if (requested == null || requested.isEmpty()) {
            BigDecimal flat = request.getProposedRate();
            if (flat == null) {
                throw new IllegalArgumentException("Vui lòng nhập học phí đề xuất cho từng môn của lớp");
            }
            requireValidRate(flat, null);
            for (String key : subjectKeys) {
                resolved.put(key, flat);
            }
            return resolved;
        }

        for (String key : subjectKeys) {
            BigDecimal rate = requested.get(key);
            if (rate == null) {
                throw new IllegalArgumentException("Vui lòng nhập học phí đề xuất cho tất cả các môn của lớp");
            }
            requireValidRate(rate, key);
            resolved.put(key, rate);
        }
        return resolved;
    }

    private void requireValidRate(BigDecimal rate, String subjectKey) {
        String subject = subjectKey == null || OTHER_SUBJECT_KEY.equals(subjectKey)
                ? ""
                : " cho môn #" + subjectKey;
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("Học phí đề xuất" + subject + " phải lớn hơn 0");
        }
        if (rate.compareTo(MIN_RATE_PER_HOUR) < 0) {
            throw new IllegalArgumentException(
                    "Học phí đề xuất" + subject + " tối thiểu " + MIN_RATE_PER_HOUR.longValue() + "đ/giờ");
        }
    }

    /** Khóa các môn trong detailsJson.subjectIds ("other" = môn tự nhập); rỗng nếu JSON thiếu/hỏng. */
    private List<String> subjectKeysFromJson(String detailsJson) {
        if (!StringUtils.hasText(detailsJson)) {
            return List.of();
        }
        try {
            JsonNode ids = objectMapper.readTree(detailsJson).path("subjectIds");
            if (!ids.isArray()) {
                return List.of();
            }
            List<String> keys = new ArrayList<>();
            for (JsonNode id : ids) {
                keys.add(id.asText());
            }
            return keys;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /** Các môn của lớp theo khóa dùng trong detailsJson.subjectIds; lớp cũ → suy từ cột phẳng. */
    private List<String> classSubjectKeys(TutoringClass tutoringClass) {
        List<String> keys = subjectKeysFromJson(tutoringClass.getDetailsJson());
        if (!keys.isEmpty()) {
            return keys;
        }
        return tutoringClass.getSubject() != null
                ? List.of(String.valueOf(tutoringClass.getSubject().getSubjectId()))
                : List.of();
    }

    /**
     * Phần khối lớp trong tiêu đề: "Lớp 12" → " lớp 12" (tránh lặp thành "lớp Lớp 12");
     * khối không phải lớp phổ thông ("Luyện thi Đại học") thì nối bằng dấu gạch.
     */
    private String gradeSuffix(String gradeName) {
        String name = gradeName.trim();
        Matcher m = GRADE_NUMBER_PATTERN.matcher(name);
        return m.matches() ? " lớp " + m.group(1) : " - " + name;
    }

    /** Tên đầy đủ các môn của lớp (kể cả môn "Khác" tự nhập) để nêu trong tiêu đề. */
    private List<String> subjectNamesFromJson(String detailsJson) {
        List<String> keys = subjectKeysFromJson(detailsJson);
        if (keys.isEmpty()) {
            return List.of();
        }
        String otherName = "";
        try {
            otherName = objectMapper.readTree(detailsJson).path("subjectOther").asText("").trim();
        } catch (JsonProcessingException ignored) {
            // Đã parse được subjectIds ở trên nên nhánh này gần như không xảy ra.
        }
        List<String> names = new ArrayList<>();
        for (String key : keys) {
            if (OTHER_SUBJECT_KEY.equals(key)) {
                names.add(StringUtils.hasText(otherName) ? otherName : "Môn học khác");
                continue;
            }
            try {
                subjectRepository.findById(Long.valueOf(key))
                        .map(Subject::getSubjectName)
                        .ifPresent(names::add);
            } catch (NumberFormatException ignored) {
                // Khóa môn lạ → bỏ qua, tiêu đề vẫn nêu các môn còn lại.
            }
        }
        return names;
    }

    /** Mức cao nhất trong các môn — dùng cho cột proposed_rate cũ và chấm điểm AI. */
    private BigDecimal highestRate(Map<String, BigDecimal> rates, BigDecimal fallback) {
        return rates.values().stream().max(BigDecimal::compareTo).orElse(fallback);
    }

    private String writeJson(Map<String, BigDecimal> rates) {
        try {
            return objectMapper.writeValueAsString(rates);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Không đọc được học phí đề xuất", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicantResponse> listApplicants(Long classId) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        List<TutorApplication> applications =
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        // Chấm điểm AI rồi xếp hạng giảm dần; Top 5 điểm cao nhất được đánh dấu "gợi ý".
        List<ApplicantResponse> ranked = applications.stream()
                .map(app -> toApplicant(app, tutoringClass))
                .sorted(Comparator.comparingInt(ApplicantResponse::getMatchScore).reversed())
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        for (int i = 0; i < ranked.size() && i < 5; i++) {
            ranked.get(i).setRecommended(true);
        }
        return ranked;
    }

    @Override
    @Transactional
    public void chooseApplicant(Long classId, Long applicationId) {
        TutoringClass tutoringClass = requireOwnedClass(classId);
        TutorApplication chosen = tutorApplicationRepository
                .findById(applicationId)
                .filter(a -> a.getTutoringClass().getClassId().equals(tutoringClass.getClassId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển"));
        for (TutorApplication app :
                tutorApplicationRepository.findByTutoringClass_ClassId(tutoringClass.getClassId())) {
            app.setStatus(
                    app.getApplicationId().equals(applicationId)
                            ? TutorApplicationStatus.ACCEPTED
                            : TutorApplicationStatus.REJECTED);
            app.setReviewedAt(java.time.LocalDateTime.now());
        }
        tutoringClass.setStatus(TutoringClassStatus.MATCHED);
        tutoringClassRepository.save(tutoringClass);
    }

    /** Lấy lớp và đảm bảo người gọi là Client đã tạo lớp đó. */
    private TutoringClass requireOwnedClass(Long classId) {
        TutoringClass tutoringClass = findClass(classId);
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Không có quyền xem ứng viên của lớp này");
        }
        return tutoringClass;
    }

    private ApplicantResponse toApplicant(TutorApplication app, TutoringClass tutoringClass) {
        Tutor tutor = app.getTutor();
        int score = aiMatchScore(app, tutor, tutoringClass);
        return ApplicantResponse.builder()
                .applicationId(app.getApplicationId())
                .tutorId(tutor.getTutorId())
                .userId(tutor.getUser().getUserId())
                .fullName(tutor.getFullName())
                .avatar(tutor.getAvatar())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(tutor.getVerificationStatus().name())
                .proposedRate(app.getProposedRate())
                .proposedRates(readRates(app.getProposedRatesJson()))
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus().name())
                .appliedAt(app.getAppliedAt())
                .matchScore(score)
                .recommended(false)
                .build();
    }

    /** Đơn cũ (trước khi có báo giá theo môn) không có JSON → trả null, client hiện mức chung. */
    private Map<String, BigDecimal> readRates(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, BigDecimal>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Điểm gợi ý AI 0–100 cho một ứng viên, tổng hợp có trọng số:
     * đánh giá (40%), kinh nghiệm (25%), mức phí phù hợp ngân sách (20%), đã xác minh hồ sơ (15%).
     */
    private int aiMatchScore(TutorApplication app, Tutor tutor, TutoringClass tutoringClass) {
        double rating = tutor.getRatingAvg() != null ? tutor.getRatingAvg().doubleValue() / 5.0 : 0;
        double experience = Math.min((tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0) / 10.0, 1.0);
        double verified =
                switch (tutor.getVerificationStatus()) {
                    case VERIFIED -> 1.0;
                    case UNDER_VERIFY -> 0.5;
                    case REJECTED -> 0.0;
                };
        double priceFit = priceFit(app, tutor, tutoringClass);
        double total = 0.40 * clamp01(rating) + 0.25 * experience + 0.20 * priceFit + 0.15 * verified;
        return (int) Math.round(clamp01(total) * 100);
    }

    /** Mức phí gia sư đề xuất so với học phí Client mong muốn: đạt/thấp hơn = 1; cao hơn giảm dần. */
    private double priceFit(TutorApplication app, Tutor tutor, TutoringClass tutoringClass) {
        BigDecimal expected = tutoringClass.getTuitionFee();
        BigDecimal rate = app.getProposedRate() != null ? app.getProposedRate() : tutor.getHourlyRate();
        if (expected == null || expected.signum() <= 0 || rate == null || rate.signum() <= 0) {
            return 0.7; // thiếu dữ liệu giá → trung tính
        }
        if (rate.compareTo(expected) <= 0) {
            return 1.0;
        }
        return clamp01(expected.doubleValue() / rate.doubleValue());
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorSearchResponse> searchTutors(String keyword, Long subjectId) {
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return tutorRepository.findAll().stream()
                .filter(t -> !StringUtils.hasText(q)
                        || t.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || (t.getBio() != null && t.getBio().toLowerCase(Locale.ROOT).contains(q)))
                .map(this::toTutorSearch)
                .toList();
    }

    @Override
    @Transactional
    public void addFavorite(Long tutorId) {
        authHelper.requireRole(UserRole.CLIENT);
        User user = requireUser();
        Tutor tutor = tutorRepository
                .findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư"));
        if (favoriteTutorRepository.existsByUser_UserIdAndTutor_TutorId(user.getUserId(), tutorId)) {
            return;
        }
        FavoriteTutor favorite = new FavoriteTutor();
        favorite.setUser(user);
        favorite.setTutor(tutor);
        favoriteTutorRepository.save(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long tutorId) {
        authHelper.requireRole(UserRole.CLIENT);
        favoriteTutorRepository
                .findByUser_UserIdAndTutor_TutorId(authHelper.currentUserId(), tutorId)
                .ifPresent(favoriteTutorRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutorSearchResponse> getFavorites() {
        authHelper.requireRole(UserRole.CLIENT);
        return favoriteTutorRepository.findByUser_UserId(authHelper.currentUserId()).stream()
                .map(f -> toTutorSearch(f.getTutor()))
                .toList();
    }

    private TutoringClass findClass(Long classId) {
        return tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    }

    private User requireUser() {
        return userRepository
                .findById(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private void requireClient(Long userId) {
        if (clientRepository.findByUser_UserId(userId).isEmpty()) {
            throw new ForbiddenException("Chỉ phụ huynh/khách hàng mới tạo lớp học");
        }
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    private Category resolveCategory(Long id) {
        if (id == null) return null;
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
    }

    private Subject resolveSubject(Long id) {
        if (id == null) return null;
        return subjectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy môn học"));
    }

    private Grade resolveGrade(Long id) {
        if (id == null) return null;
        return gradeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khối/lớp"));
    }

    private Location resolveLocation(Long id) {
        if (id == null) return null;
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa điểm"));
    }

    private String formatLocation(Location location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(location.getDistrictName())) {
            sb.append(location.getDistrictName());
        }
        if (location.getProvince() != null && StringUtils.hasText(location.getProvince().getProvinceName())) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(location.getProvince().getProvinceName());
        }
        return sb.length() > 0 ? sb.toString() : location.getAddressLine();
    }

    private ClassResponse toClassResponse(TutoringClass c) {
        Client client = clientRepository.findByUser_UserId(c.getCreator().getUserId()).orElse(null);
        return ClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .description(c.getDescription())
                .detailsJson(c.getDetailsJson())
                .creatorId(c.getCreator().getUserId())
                .creatorName(client != null ? client.getFullName() : c.getCreator().getEmail())
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeId(c.getGrade() != null ? c.getGrade().getGradeId() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .learningGoal(c.getLearningGoal())
                .tutorRequirement(c.getTutorRequirement())
                .locationId(c.getLocation() != null ? c.getLocation().getLocationId() : null)
                .locationName(formatLocation(c.getLocation()))
                .address(c.getAddress())
                .lessonMode(c.getLessonMode())
                .numberOfSessions(c.getNumberOfSessions())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .tuitionFee(c.getTuitionFee())
                .budget(c.getBudget())
                .recurringType(c.getRecurringType())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .applicationCount(tutorApplicationRepository.countByTutoringClass_ClassId(c.getClassId()))
                .build();
    }

    private TutorSearchResponse toTutorSearch(Tutor tutor) {
        return TutorSearchResponse.builder()
                .tutorId(tutor.getTutorId())
                .userId(tutor.getUser().getUserId())
                .fullName(tutor.getFullName())
                .bio(tutor.getBio())
                .experienceYears(tutor.getExperienceYears())
                .hourlyRate(tutor.getHourlyRate())
                .ratingAvg(tutor.getRatingAvg())
                .verificationStatus(tutor.getVerificationStatus().name())
                .build();
    }
}
