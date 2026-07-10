package com.tcs.module.tutor.service.impl;

import com.tcs.exception.ForbiddenException;
import com.tcs.exception.ResourceNotFoundException;
import com.tcs.module.center.dto.request.MarkAttendanceRequest;
import com.tcs.module.center.dto.response.CenterScheduleClassResponse;
import com.tcs.module.center.dto.response.ScheduleSlotResponse;
import com.tcs.module.center.dto.response.StudentAttendanceResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Override
    @Transactional(readOnly = true)
    public List<CenterScheduleClassResponse> getSchedule(LocalDate date) {
        Tutor tutor = requireTutor();
        LocalDate d = date != null ? date : LocalDate.now();
        int weekday = d.getDayOfWeek().getValue();
        List<CenterScheduleClassResponse> result = new ArrayList<>();
        for (ClassAssignment a : classAssignmentRepository
                .findByTutor_TutorIdAndStatus(tutor.getTutorId(), ClassAssignmentStatus.ACTIVE)) {
            if (a.getApplication() == null) {
                continue;
            }
            TutoringClass c = a.getApplication().getTutoringClass();
            if (c == null || c.getStartDate() == null || c.getEndDate() == null
                    || d.isBefore(c.getStartDate()) || d.isAfter(c.getEndDate())) {
                continue;
            }
            CenterScheduleClassResponse item = buildScheduleItem(c, d, weekday, tutor);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
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
        int weekday = d.getDayOfWeek().getValue();
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
        int weekday = d.getDayOfWeek().getValue();
        CenterScheduleClassResponse item = buildScheduleItem(c, d, weekday, tutor);
        if (item == null) {
            throw new IllegalArgumentException("Lớp không có buổi học vào ngày này");
        }
        return item;
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
        int weekday = d.getDayOfWeek().getValue();
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
