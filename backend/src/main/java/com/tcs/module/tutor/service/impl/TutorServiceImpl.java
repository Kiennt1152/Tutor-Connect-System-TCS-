package com.tcs.module.tutor.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.center.dto.request.RescheduleRequestBody;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.RescheduleResponse;
import com.tcs.module.center.dto.response.ScheduleSlotResponse;
import com.tcs.module.center.dto.response.StudentAttendanceResponse;
import com.tcs.module.marketplace.dto.RescheduleEntry;
import com.tcs.module.marketplace.service.RescheduleService;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.LessonAttendance;
import com.tcs.module.marketplace.entity.ScheduleSlot;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.LessonAttendanceRepository;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.profile.entity.Tutor;
import com.tcs.module.profile.enums.UserRole;
import com.tcs.module.profile.repository.TutorRepository;
import com.tcs.module.tutor.service.TutorService;
import com.tcs.security.AuthHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final AuthHelper authHelper;
    private final TutorRepository tutorRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final LessonRepository lessonRepository;
    private final LessonAttendanceRepository lessonAttendanceRepository;
    private final RescheduleService rescheduleService;

    private static final DateTimeFormatter D_MM = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    @Transactional(readOnly = true)
    public List<CenterScheduleClassResponse> getSchedule(LocalDate date) {
        Tutor tutor = requireTutor();
        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = d.getDayOfWeek().getValue();

        Map<Long, TutoringClass> myClasses = new HashMap<>();
        for (ClassAssignment a : classAssignmentRepository
                .findByTutor_TutorIdAndStatus(tutor.getTutorId(), ClassAssignmentStatus.ACTIVE)) {
            if (a.getApplication() == null || a.getApplication().getTutoringClass() == null) {
                continue;
            }
            TutoringClass c = a.getApplication().getTutoringClass();
            myClasses.put(c.getClassId(), c);
        }

        List<RescheduleEntry> approved = rescheduleService.listApprovedByClassIds(myClasses.keySet());
        Set<Long> movedAway = approved.stream()
                .filter(e -> e.originalDate().equals(d))
                .map(RescheduleEntry::classId)
                .collect(java.util.stream.Collectors.toSet());

        List<CenterScheduleClassResponse> result = new ArrayList<>();
        // Buổi thường trong ngày (bỏ qua buổi đã bị dời đi).
        for (TutoringClass c : myClasses.values()) {
            if (c.getStartDate() == null || c.getEndDate() == null
                    || d.isBefore(c.getStartDate()) || d.isAfter(c.getEndDate())
                    || movedAway.contains(c.getClassId())) {
                continue;
            }
            CenterScheduleClassResponse item = buildScheduleItem(c, d, weekday, tutor);
            if (item != null) {
                result.add(item);
            }
        }
        // Buổi được dời TỚI ngày này (lấy khung giờ theo thứ của ngày gốc).
        for (RescheduleEntry e : approved) {
            if (!e.newDate().equals(d)) {
                continue;
            }
            TutoringClass c = myClasses.get(e.classId());
            if (c == null) {
                continue;
            }
            CenterScheduleClassResponse item =
                    buildScheduleItem(c, d, e.originalDate().getDayOfWeek().getValue(), tutor);
            if (item != null) {
                applyNewTimes(item, e, d);
                item.setRescheduled(true);
                item.setRescheduleNote("Dời từ " + e.originalDate().format(D_MM));
                result.add(item);
            }
        }
        return result;
    }

    /** Nếu buổi dời có khung giờ mới thì thay khung giờ hiển thị bằng khung giờ đó. */
    private void applyNewTimes(CenterScheduleClassResponse item, RescheduleEntry e, LocalDate d) {
        if (e.newStartTime() != null && e.newEndTime() != null) {
            item.setSlots(List.of(ScheduleSlotResponse.builder()
                    .dayOfWeek(d.getDayOfWeek().getValue())
                    .startTime(e.newStartTime())
                    .endTime(e.newEndTime())
                    .build()));
        }
    }

    @Override
    @Transactional
    public RescheduleResponse requestReschedule(Long classId, RescheduleRequestBody body) {
        Tutor tutor = requireTutor();
        TutoringClass c = tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        requireAssigned(classId, tutor);
        if (body.getOriginalDate() == null || body.getNewDate() == null) {
            throw new IllegalArgumentException("Vui lòng chọn ngày cần dời và ngày mới");
        }
        LocalDate original = body.getOriginalDate();
        LocalDate next = body.getNewDate();
        if (next.equals(original)) {
            throw new IllegalArgumentException("Ngày mới phải khác ngày cần dời");
        }
        if (next.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày mới phải từ hôm nay trở đi");
        }
        // Buổi cần dời phải là một buổi học thật của lớp (có tiết đúng thứ, trong khoảng ngày).
        if (c.getStartDate() == null || c.getEndDate() == null
                || original.isBefore(c.getStartDate()) || original.isAfter(c.getEndDate())
                || slotsOn(classId, original.getDayOfWeek().getValue()).isEmpty()) {
            throw new IllegalArgumentException("Ngày cần dời không phải buổi học của lớp");
        }
        // Ngày mới nên nằm trong khoảng ngày của lớp.
        if (next.isBefore(c.getStartDate()) || next.isAfter(c.getEndDate())) {
            throw new IllegalArgumentException("Ngày mới phải nằm trong khoảng thời gian của lớp");
        }
        LocalTime start = parseTime(body.getNewStartTime());
        LocalTime end = parseTime(body.getNewEndTime());
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Khung giờ mới không hợp lệ (giờ kết thúc phải sau giờ bắt đầu)");
        }
        // Không cho trùng khung giờ với các buổi dạy khác của gia sư trong ngày mới.
        if (hasTimeConflict(tutor, next, start, end, classId, original)) {
            throw new IllegalArgumentException(
                    "Khung giờ mới bị trùng với buổi dạy khác của bạn trong ngày " + next.format(D_MM));
        }
        RescheduleEntry entry = rescheduleService.request(
                classId, original, next, start, end, tutor.getTutorId(), body.getReason());
        return toRescheduleResponse(entry, c, tutor.getFullName());
    }

    /** Các buổi dạy (khoảng giờ) của gia sư trong một ngày có bị trùng với [start,end] không. */
    private boolean hasTimeConflict(
            Tutor tutor, LocalDate date, LocalTime start, LocalTime end,
            Long excludeClassId, LocalDate excludeOriginalDate) {
        int wd = date.getDayOfWeek().getValue();
        Map<Long, TutoringClass> myClasses = myActiveClasses(tutor);
        List<RescheduleEntry> approved = rescheduleService.listApprovedByClassIds(myClasses.keySet());
        Set<Long> movedAwayOnDate = approved.stream()
                .filter(e -> e.originalDate().equals(date))
                .map(RescheduleEntry::classId)
                .collect(Collectors.toSet());

        List<LocalTime[]> occupied = new ArrayList<>();
        // Buổi thường trong ngày (bỏ buổi đã dời đi).
        for (TutoringClass c : myClasses.values()) {
            if (c.getStartDate() == null || c.getEndDate() == null
                    || date.isBefore(c.getStartDate()) || date.isAfter(c.getEndDate())
                    || movedAwayOnDate.contains(c.getClassId())) {
                continue;
            }
            for (ScheduleSlot s : slotsOn(c.getClassId(), wd)) {
                occupied.add(new LocalTime[] {s.getStartTime(), s.getEndTime()});
            }
        }
        // Buổi đã được duyệt dời TỚI ngày này (trừ chính yêu cầu đang xét).
        for (RescheduleEntry e : approved) {
            if (!e.newDate().equals(date)) {
                continue;
            }
            if (e.classId().equals(excludeClassId) && e.originalDate().equals(excludeOriginalDate)) {
                continue;
            }
            if (e.newStartTime() != null && e.newEndTime() != null) {
                occupied.add(new LocalTime[] {e.newStartTime(), e.newEndTime()});
            } else {
                for (ScheduleSlot s : slotsOn(e.classId(), e.originalDate().getDayOfWeek().getValue())) {
                    occupied.add(new LocalTime[] {s.getStartTime(), s.getEndTime()});
                }
            }
        }
        for (LocalTime[] r : occupied) {
            if (r[0].isBefore(end) && start.isBefore(r[1])) {
                return true;
            }
        }
        return false;
    }

    /** Parse "HH:mm" (hoặc "HH:mm:ss") -> LocalTime; trả null nếu rỗng/không hợp lệ. */
    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Khung giờ không hợp lệ: " + value);
        }
    }

    private Map<Long, TutoringClass> myActiveClasses(Tutor tutor) {
        Map<Long, TutoringClass> map = new HashMap<>();
        for (ClassAssignment a : classAssignmentRepository
                .findByTutor_TutorIdAndStatus(tutor.getTutorId(), ClassAssignmentStatus.ACTIVE)) {
            if (a.getApplication() != null && a.getApplication().getTutoringClass() != null) {
                TutoringClass c = a.getApplication().getTutoringClass();
                map.put(c.getClassId(), c);
            }
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleResponse> listMyReschedules() {
        Tutor tutor = requireTutor();
        Map<Long, TutoringClass> myClasses = new HashMap<>();
        for (ClassAssignment a : classAssignmentRepository
                .findByTutor_TutorIdAndStatus(tutor.getTutorId(), ClassAssignmentStatus.ACTIVE)) {
            if (a.getApplication() != null && a.getApplication().getTutoringClass() != null) {
                TutoringClass c = a.getApplication().getTutoringClass();
                myClasses.put(c.getClassId(), c);
            }
        }
        return rescheduleService.listByClassIds(myClasses.keySet()).stream()
                .map(e -> toRescheduleResponse(e, myClasses.get(e.classId()), tutor.getFullName()))
                .toList();
    }

    private RescheduleResponse toRescheduleResponse(RescheduleEntry e, TutoringClass c, String tutorName) {
        return RescheduleResponse.builder()
                .classId(e.classId())
                .className(c != null ? c.getTitle() : null)
                .originalDate(e.originalDate())
                .newDate(e.newDate())
                .newStartTime(e.newStartTime())
                .newEndTime(e.newEndTime())
                .status(e.status())
                .tutorId(e.tutorId())
                .tutorName(tutorName)
                .reason(e.reason())
                .build();
    }

    @Override
    @Transactional
    public CenterScheduleClassResponse markAttendance(
            Long classId, LocalDate date, Long classStudentId, LessonAttendanceStatus status) {
        Tutor tutor = requireTutor();
        TutoringClass tutoringClass = tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        if (classStudentId == null || status == null) {
            throw new IllegalArgumentException("Thiếu thông tin điểm danh");
        }

        // Chỉ gia sư đang phụ trách lớp này mới được điểm danh.
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Bạn không phụ trách lớp này"));
        if (!assignment.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Bạn không phụ trách lớp này");
        }

        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = slotWeekday(d, arrivingReschedule(classId, d));
        List<ScheduleSlot> slotsToday = slotsOn(classId, weekday);
        if (slotsToday.isEmpty()) {
            throw new IllegalArgumentException("Lớp không có buổi học vào ngày này");
        }

        ClassStudent student = classStudentRepository
                .findById(classStudentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));
        if (!student.getTutoringClass().getClassId().equals(classId)) {
            throw new IllegalArgumentException("Học sinh không thuộc lớp này");
        }

        ScheduleSlot repSlot = slotsToday.get(0);
        int seq = sessionSequence(tutoringClass.getStartDate(), d);
        Lesson lesson = lessonRepository
                .findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(classId, repSlot.getSlotId(), seq)
                .orElseGet(() -> {
                    Lesson l = new Lesson();
                    l.setTutoringClass(tutoringClass);
                    l.setSlot(repSlot);
                    l.setSequenceNo(seq);
                    l.setTutor(tutor);
                    return lessonRepository.save(l);
                });

        LessonAttendance attendance = lessonAttendanceRepository
                .findFirstByLesson_LessonIdAndClassStudent_ClassStudentId(lesson.getLessonId(), classStudentId)
                .orElseGet(() -> {
                    LessonAttendance a = new LessonAttendance();
                    a.setLesson(lesson);
                    a.setClassStudent(student);
                    return a;
                });
        attendance.setStatus(status);
        lessonAttendanceRepository.save(attendance);

        return buildScheduleItem(tutoringClass, d, weekday, tutor);
    }

    @Override
    @Transactional(readOnly = true)
    public CenterScheduleClassResponse getClassSession(Long classId, LocalDate date) {
        Tutor tutor = requireTutor();
        TutoringClass c = tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        requireAssigned(classId, tutor);
        LocalDate d = date != null ? date : LocalDate.now();
        RescheduleEntry arriving = arrivingReschedule(classId, d);
        int weekday = slotWeekday(d, arriving);
        CenterScheduleClassResponse item = buildScheduleItem(c, d, weekday, tutor);
        if (item == null) {
            throw new IllegalArgumentException("Lớp không có buổi học vào ngày này");
        }
        if (arriving != null) {
            applyNewTimes(item, arriving, d);
            item.setRescheduled(true);
            item.setRescheduleNote("Dời từ " + arriving.originalDate().format(D_MM));
        }
        return item;
    }

    /** Nếu ngày là buổi được dời tới (APPROVED) thì trả về ngoại lệ đó, ngược lại null. */
    private RescheduleEntry arrivingReschedule(Long classId, LocalDate d) {
        for (RescheduleEntry e : rescheduleService.listApprovedByClassIds(List.of(classId))) {
            if (e.newDate().equals(d)) {
                return e;
            }
        }
        return null;
    }

    /** Thứ dùng để lấy khung giờ: buổi dời tới lấy theo thứ của ngày gốc. */
    private int slotWeekday(LocalDate d, RescheduleEntry arriving) {
        return arriving != null
                ? arriving.originalDate().getDayOfWeek().getValue()
                : d.getDayOfWeek().getValue();
    }

    @Override
    @Transactional
    public CenterScheduleClassResponse markAttendanceBatch(
            Long classId, LocalDate date, List<MarkAttendanceRequest> records) {
        Tutor tutor = requireTutor();
        TutoringClass tutoringClass = tutoringClassRepository
                .findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
        requireAssigned(classId, tutor);
        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException("Chưa có dữ liệu điểm danh");
        }
        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = slotWeekday(d, arrivingReschedule(classId, d));
        List<ScheduleSlot> slotsToday = slotsOn(classId, weekday);
        if (slotsToday.isEmpty()) {
            throw new IllegalArgumentException("Lớp không có buổi học vào ngày này");
        }
        ScheduleSlot repSlot = slotsToday.get(0);
        int seq = sessionSequence(tutoringClass.getStartDate(), d);

        // Chỉ điểm danh MỘT LẦN: nếu buổi đã có điểm danh thì chặn.
        Lesson existing = lessonRepository
                .findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(classId, repSlot.getSlotId(), seq)
                .orElse(null);
        if (existing != null
                && !lessonAttendanceRepository.findByLesson_LessonId(existing.getLessonId()).isEmpty()) {
            throw new IllegalArgumentException("Buổi học này đã được điểm danh, không thể điểm danh lại");
        }

        Lesson lesson = existing != null ? existing : lessonRepository.save(newLesson(tutoringClass, repSlot, seq, tutor));

        for (MarkAttendanceRequest r : records) {
            if (r.getClassStudentId() == null || r.getStatus() == null) {
                continue;
            }
            ClassStudent student = classStudentRepository
                    .findById(r.getClassStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học sinh"));
            if (!student.getTutoringClass().getClassId().equals(classId)) {
                throw new IllegalArgumentException("Học sinh không thuộc lớp này");
            }
            LessonAttendance att = lessonAttendanceRepository
                    .findFirstByLesson_LessonIdAndClassStudent_ClassStudentId(
                            lesson.getLessonId(), r.getClassStudentId())
                    .orElseGet(() -> {
                        LessonAttendance a = new LessonAttendance();
                        a.setLesson(lesson);
                        a.setClassStudent(student);
                        return a;
                    });
            att.setStatus(r.getStatus());
            lessonAttendanceRepository.save(att);
        }
        return buildScheduleItem(tutoringClass, d, weekday, tutor);
    }

    private Lesson newLesson(TutoringClass c, ScheduleSlot slot, int seq, Tutor tutor) {
        Lesson l = new Lesson();
        l.setTutoringClass(c);
        l.setSlot(slot);
        l.setSequenceNo(seq);
        l.setTutor(tutor);
        return l;
    }

    private void requireAssigned(Long classId, Tutor tutor) {
        ClassAssignment assignment = classAssignmentRepository
                .findFirstByApplication_TutoringClass_ClassIdAndStatus(classId, ClassAssignmentStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Bạn không phụ trách lớp này"));
        if (!assignment.getTutor().getTutorId().equals(tutor.getTutorId())) {
            throw new ForbiddenException("Bạn không phụ trách lớp này");
        }
    }

    private Tutor requireTutor() {
        authHelper.requireRole(UserRole.TUTOR);
        return tutorRepository
                .findByUser_UserId(authHelper.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư"));
    }

    private List<ScheduleSlot> slotsOn(Long classId, int weekday) {
        return scheduleSlotRepository.findByTutoringClass_ClassId(classId).stream()
                .filter(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == weekday)
                .sorted(Comparator.comparing(ScheduleSlot::getStartTime))
                .toList();
    }

    private int sessionSequence(LocalDate start, LocalDate date) {
        return (int) Math.max(0, ChronoUnit.DAYS.between(start, date));
    }

    private CenterScheduleClassResponse buildScheduleItem(
            TutoringClass c, LocalDate date, int weekday, Tutor tutor) {
        List<ScheduleSlot> slotsToday = slotsOn(c.getClassId(), weekday);
        if (slotsToday.isEmpty()) {
            return null;
        }

        List<ClassStudent> students = classStudentRepository
                .findByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED);

        Map<Long, String> attendanceByStudent = new HashMap<>();
        ScheduleSlot repSlot = slotsToday.get(0);
        int seq = sessionSequence(c.getStartDate(), date);
        lessonRepository
                .findFirstByTutoringClass_ClassIdAndSlot_SlotIdAndSequenceNo(
                        c.getClassId(), repSlot.getSlotId(), seq)
                .ifPresent(lesson -> lessonAttendanceRepository.findByLesson_LessonId(lesson.getLessonId())
                        .forEach(a -> attendanceByStudent.put(
                                a.getClassStudent().getClassStudentId(), a.getStatus().name())));

        List<StudentAttendanceResponse> studentItems = students.stream()
                .map(s -> StudentAttendanceResponse.builder()
                        .classStudentId(s.getClassStudentId())
                        .studentName(s.getStudentName())
                        .studentPhone(s.getStudentPhone())
                        .status(attendanceByStudent.get(s.getClassStudentId()))
                        .build())
                .toList();

        List<ScheduleSlotResponse> slotResponses = slotsToday.stream()
                .map(s -> ScheduleSlotResponse.builder()
                        .slotId(s.getSlotId())
                        .dayOfWeek(s.getDayOfWeek())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();

        return CenterScheduleClassResponse.builder()
                .classId(c.getClassId())
                .title(c.getTitle())
                .subjectName(c.getSubject() != null ? c.getSubject().getSubjectName() : null)
                .gradeName(c.getGrade() != null ? c.getGrade().getGradeName() : null)
                .lessonMode(c.getLessonMode())
                .slots(slotResponses)
                .assignedTutorId(tutor.getTutorId())
                .assignedTutorName(tutor.getFullName())
                .studentCount(students.size())
                .students(studentItems)
                .attendanceTaken(!attendanceByStudent.isEmpty())
                .build();
    }
}
