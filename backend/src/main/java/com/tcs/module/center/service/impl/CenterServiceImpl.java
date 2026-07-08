package com.tcs.module.center.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.center.dto.request.ApplyRecruitmentRequest;
import com.tcs.module.center.dto.request.CreateRecruitmentPostRequest;
import com.tcs.module.center.dto.request.SaveClassRequest;
import com.tcs.module.center.dto.request.ScheduleSlotRequest;
import com.tcs.module.center.dto.response.CenterClassResponse;
import com.tcs.module.center.dto.response.RecruitmentPostResponse;
import com.tcs.module.center.dto.response.ScheduleSlotResponse;
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
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.repository.TutorCenterRepository;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.security.AuthHelper;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final ProvinceRepository provinceRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

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

    // ===================== UC-14-B: Manage Classes (Tutor Center) =====================

    @Override
    @Transactional(readOnly = true)
    public List<CenterClassResponse> listMyClasses() {
        requireCenter();
        return tutoringClassRepository.findByCreator_UserId(authHelper.currentUserId()).stream()
                .map(this::toClassResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CenterClassResponse getMyClass(Long classId) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass); // BR-07 / AF-04
        return toClassResponse(tutoringClass);
    }

    @Override
    @Transactional
    public CenterClassResponse createClass(SaveClassRequest request) {
        TutorCenter center = requireCenter();
        validate(request);

        TutoringClass tutoringClass = new TutoringClass();
        tutoringClass.setCreator(center.getUser());
        tutoringClass.setCenter(center);
        tutoringClass.setClassType(ClassType.CENTER);
        tutoringClass.setStatus(TutoringClassStatus.DRAFT); // BR-05
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);

        replaceScheduleSlots(saved, request.getSchedule());
        return toClassResponse(saved);
    }

    @Override
    @Transactional
    public CenterClassResponse updateClass(Long classId, SaveClassRequest request) {
        requireCenter();
        TutoringClass tutoringClass = findClass(classId);
        requireOwner(tutoringClass); // BR-07 / AF-04
        // BR-06 / AF-03: chỉ được sửa khi lớp ở trạng thái DRAFT hoặc OPEN.
        if (tutoringClass.getStatus() != TutoringClassStatus.DRAFT
                && tutoringClass.getStatus() != TutoringClassStatus.OPEN) {
            throw new IllegalArgumentException("Lớp học này không thể chỉnh sửa nữa.");
        }
        validate(request);
        applyFields(tutoringClass, request);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);

        replaceScheduleSlots(saved, request.getSchedule());
        return toClassResponse(saved);
    }

    private void validate(SaveClassRequest request) {
        // BR-01: các trường bắt buộc.
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("Tiêu đề là bắt buộc");
        }
        if (!StringUtils.hasText(request.getCategoryName())) {
            throw new IllegalArgumentException("Danh mục là bắt buộc");
        }
        if (!StringUtils.hasText(request.getSubjectName())) {
            throw new IllegalArgumentException("Môn học là bắt buộc");
        }
        if (!StringUtils.hasText(request.getGradeName())) {
            throw new IllegalArgumentException("Khối/lớp là bắt buộc");
        }
        if (!StringUtils.hasText(request.getLocationText())) {
            throw new IllegalArgumentException("Địa điểm là bắt buộc");
        }
        if (request.getLessonMode() == null) {
            throw new IllegalArgumentException("Hình thức học là bắt buộc");
        }
        if (request.getRecurringType() == null) {
            throw new IllegalArgumentException("Kiểu lặp lịch là bắt buộc");
        }
        // BR-03: số buổi là số nguyên dương.
        if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
            throw new IllegalArgumentException("Số buổi học phải là số nguyên dương");
        }
        // BR-04: học phí là số dương.
        if (request.getTuitionFee() == null || request.getTuitionFee().signum() <= 0) {
            throw new IllegalArgumentException("Học phí phải là số dương");
        }
        // Ngày bắt đầu/kết thúc bắt buộc + BR-02.
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc là bắt buộc");
        }
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        // Lịch học: tối thiểu một khung, mỗi khung hợp lệ.
        if (request.getSchedule() == null || request.getSchedule().isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một khung lịch học");
        }
        List<ScheduleSlotRequest> slots = request.getSchedule();
        for (ScheduleSlotRequest slot : slots) {
            if (slot.getDayOfWeek() == null || slot.getDayOfWeek() < 1 || slot.getDayOfWeek() > 7) {
                throw new IllegalArgumentException("Thứ trong tuần của khung lịch không hợp lệ (1-7)");
            }
            if (slot.getStartTime() == null || slot.getEndTime() == null
                    || !slot.getEndTime().isAfter(slot.getStartTime())) {
                throw new IllegalArgumentException("Giờ kết thúc của khung lịch phải sau giờ bắt đầu");
            }
        }
        // Không cho hai khung trùng/chồng giờ trong cùng một ngày.
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ScheduleSlotRequest a = slots.get(i);
                ScheduleSlotRequest b = slots.get(j);
                if (a.getDayOfWeek().equals(b.getDayOfWeek())
                        && a.getStartTime().isBefore(b.getEndTime())
                        && b.getStartTime().isBefore(a.getEndTime())) {
                    throw new IllegalArgumentException(
                            "Lịch học bị trùng/chồng giờ giữa các khung trong cùng một ngày");
                }
            }
        }
    }

    private void applyFields(TutoringClass tutoringClass, SaveClassRequest request) {
        tutoringClass.setTitle(request.getTitle().trim());
        tutoringClass.setDescription(
                StringUtils.hasText(request.getDescription()) ? request.getDescription() : "");
        tutoringClass.setCategory(resolveOrCreateCategory(request.getCategoryName()));
        tutoringClass.setSubject(resolveOrCreateSubject(request.getSubjectName()));
        tutoringClass.setGrade(resolveOrCreateGrade(request.getGradeName()));
        tutoringClass.setLocation(resolveOrCreateLocation(request.getLocationText()));
        tutoringClass.setLessonMode(request.getLessonMode());
        tutoringClass.setNumberOfSessions(request.getNumberOfSessions());
        tutoringClass.setRecurringType(request.getRecurringType());
        tutoringClass.setStartDate(request.getStartDate());
        tutoringClass.setEndDate(request.getEndDate());
        tutoringClass.setTuitionFee(request.getTuitionFee());
    }

    private void replaceScheduleSlots(TutoringClass tutoringClass, List<ScheduleSlotRequest> slots) {
        List<ScheduleSlot> existing =
                scheduleSlotRepository.findByTutoringClass_ClassId(tutoringClass.getClassId());
        if (!existing.isEmpty()) {
            scheduleSlotRepository.deleteAll(existing);
        }
        if (slots == null) {
            return;
        }
        List<ScheduleSlot> toSave = new ArrayList<>();
        for (ScheduleSlotRequest req : slots) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setTutoringClass(tutoringClass);
            slot.setDayOfWeek(req.getDayOfWeek());
            slot.setStartTime(req.getStartTime());
            slot.setEndTime(req.getEndTime());
            toSave.add(slot);
        }
        scheduleSlotRepository.saveAll(toSave);
    }

    private void requireOwner(TutoringClass tutoringClass) {
        if (!tutoringClass.getCreator().getUserId().equals(authHelper.currentUserId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa lớp học này");
        }
    }

    private TutoringClass findClass(Long classId) {
        return tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    }

    // Tìm-hoặc-tạo theo tên người dùng tự nhập (giữ FK toàn vẹn mà không cần dropdown).
    private Category resolveOrCreateCategory(String name) {
        String n = name.trim();
        return categoryRepository.findFirstByNameIgnoreCase(n).orElseGet(() -> {
            Category c = new Category();
            c.setName(n);
            return categoryRepository.save(c);
        });
    }

    private Subject resolveOrCreateSubject(String name) {
        String n = name.trim();
        return subjectRepository.findFirstBySubjectNameIgnoreCase(n).orElseGet(() -> {
            Subject s = new Subject();
            s.setSubjectName(n);
            return subjectRepository.save(s);
        });
    }

    private Grade resolveOrCreateGrade(String name) {
        String n = name.trim();
        return gradeRepository.findFirstByGradeNameIgnoreCase(n).orElseGet(() -> {
            Grade g = new Grade();
            g.setGradeName(n);
            return gradeRepository.save(g);
        });
    }

    private Location resolveOrCreateLocation(String text) {
        String n = text.trim();
        return locationRepository.findFirstByAddressLineIgnoreCase(n).orElseGet(() -> {
            Location loc = new Location();
            loc.setAddressLine(n);
            loc.setProvince(resolveOrCreateProvince("Khác"));
            return locationRepository.save(loc);
        });
    }

    private Province resolveOrCreateProvince(String name) {
        String n = name.trim();
        return provinceRepository.findFirstByProvinceNameIgnoreCase(n).orElseGet(() -> {
            Province p = new Province();
            p.setProvinceName(n);
            return provinceRepository.save(p);
        });
    }

    private CenterClassResponse toClassResponse(TutoringClass c) {
        List<ScheduleSlotResponse> schedule =
                scheduleSlotRepository.findByTutoringClass_ClassId(c.getClassId()).stream()
                        .map(s -> ScheduleSlotResponse.builder()
                                .slotId(s.getSlotId())
                                .dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime())
                                .endTime(s.getEndTime())
                                .build())
                        .toList();
        return CenterClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .description(c.getDescription())
                .creatorId(c.getCreator().getUserId())
                .centerId(c.getCenter() != null ? c.getCenter().getCenterId() : null)
                .categoryId(c.getCategory() != null ? c.getCategory().getCategoryId() : null)
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeId(c.getGrade() != null ? c.getGrade().getGradeId() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .locationId(c.getLocation() != null ? c.getLocation().getLocationId() : null)
                .locationLabel(locationLabel(c.getLocation()))
                .locationText(c.getLocation() != null ? c.getLocation().getAddressLine() : null)
                .lessonMode(c.getLessonMode())
                .numberOfSessions(c.getNumberOfSessions())
                .recurringType(c.getRecurringType())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .tuitionFee(c.getTuitionFee())
                .status(c.getStatus())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .schedule(schedule)
                .build();
    }

    private String locationLabel(Location location) {
        if (location == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(location.getAddressLine());
        if (StringUtils.hasText(location.getDistrictName())) {
            sb.append(", ").append(location.getDistrictName());
        }
        return sb.toString();
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
