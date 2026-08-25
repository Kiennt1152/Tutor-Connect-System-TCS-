package com.tcs.module.center.dto.response;

import com.tcs.module.marketplace.enums.LessonMode;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một lớp có buổi học trong ngày, kèm gia sư, danh sách học sinh và điểm danh. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterScheduleClassResponse {

    private Long classId;
    private String title;
    private String subjectName;
    private String gradeName;
    private LessonMode lessonMode;
    private List<ScheduleSlotResponse> slots; // các khung giờ trong ngày
    private Long assignedTutorId;
    private String assignedTutorName;
    private int studentCount;
    private List<StudentAttendanceResponse> students;
    /** true nếu buổi này đã được điểm danh (đã khoá, không điểm danh lại). */
    private boolean attendanceTaken;
    /** true nếu buổi này là buổi được dời tới (từ ngày khác). */
    private boolean rescheduled;
    /** Ghi chú dời lịch, VD: "Dời từ 20/07". */
    private String rescheduleNote;

    /** true nếu buổi này được gia sư phụ dạy thay (yêu cầu đã duyệt). */
    private boolean substituted;
    /** Ghi chú dạy thay, VD: "Dạy thay: Nguyễn Văn A" hoặc "Bạn dạy thay cho Trần B". */
    private String substituteNote;
    /**
     * true nếu (trong góc nhìn gia sư chính) buổi này đã được bàn giao cho gia sư phụ —
     * gia sư chính không điểm danh/không xin đổi lịch buổi này nữa.
     */
    private boolean handedOff;
    /** Gia sư phụ của lớp (nếu có) — để gia sư chính biết có thể nhờ dạy thay hay không. */
    private Long assistantTutorId;
    private String assistantTutorName;

    /** true nếu đây là buổi CUỐI của khóa (đủ số buổi) — nơi hiện nút "Xác nhận hoàn thành". */
    private boolean finalSession;
    /** true nếu lớp đã được xác nhận hoàn thành (COMPLETED). */
    private boolean classCompleted;
    /** true nếu GIA SƯ đã xác nhận hoàn thành (đang chờ trung tâm đóng lớp). */
    private boolean tutorCompletionConfirmed;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getGradeName() { return gradeName; }
    public void setGradeName(String gradeName) { this.gradeName = gradeName; }
    public LessonMode getLessonMode() { return lessonMode; }
    public void setLessonMode(LessonMode lessonMode) { this.lessonMode = lessonMode; }
    public List<ScheduleSlotResponse> getSlots() { return slots; }
    public void setSlots(List<ScheduleSlotResponse> slots) { this.slots = slots; }
    public Long getAssignedTutorId() { return assignedTutorId; }
    public void setAssignedTutorId(Long assignedTutorId) { this.assignedTutorId = assignedTutorId; }
    public String getAssignedTutorName() { return assignedTutorName; }
    public void setAssignedTutorName(String assignedTutorName) { this.assignedTutorName = assignedTutorName; }
    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
    public List<StudentAttendanceResponse> getStudents() { return students; }
    public void setStudents(List<StudentAttendanceResponse> students) { this.students = students; }
    public boolean isAttendanceTaken() { return attendanceTaken; }
    public void setAttendanceTaken(boolean attendanceTaken) { this.attendanceTaken = attendanceTaken; }
    public boolean isRescheduled() { return rescheduled; }
    public void setRescheduled(boolean rescheduled) { this.rescheduled = rescheduled; }
    public String getRescheduleNote() { return rescheduleNote; }
    public void setRescheduleNote(String rescheduleNote) { this.rescheduleNote = rescheduleNote; }
    public boolean isSubstituted() { return substituted; }
    public void setSubstituted(boolean substituted) { this.substituted = substituted; }
    public String getSubstituteNote() { return substituteNote; }
    public void setSubstituteNote(String substituteNote) { this.substituteNote = substituteNote; }
    public boolean isHandedOff() { return handedOff; }
    public void setHandedOff(boolean handedOff) { this.handedOff = handedOff; }
    public Long getAssistantTutorId() { return assistantTutorId; }
    public void setAssistantTutorId(Long assistantTutorId) { this.assistantTutorId = assistantTutorId; }
    public String getAssistantTutorName() { return assistantTutorName; }
    public void setAssistantTutorName(String assistantTutorName) { this.assistantTutorName = assistantTutorName; }
    public boolean isFinalSession() { return finalSession; }
    public void setFinalSession(boolean finalSession) { this.finalSession = finalSession; }
    public boolean isClassCompleted() { return classCompleted; }
    public void setClassCompleted(boolean classCompleted) { this.classCompleted = classCompleted; }
    public boolean isTutorCompletionConfirmed() { return tutorCompletionConfirmed; }
    public CenterScheduleClassResponse() {}

    public CenterScheduleClassResponse(Long classId, String title, String subjectName, String gradeName, LessonMode lessonMode, List<ScheduleSlotResponse> slots, Long assignedTutorId, String assignedTutorName, int studentCount, List<StudentAttendanceResponse> students, boolean attendanceTaken, boolean rescheduled, String rescheduleNote, boolean substituted, String substituteNote, boolean handedOff, Long assistantTutorId, String assistantTutorName, boolean finalSession, boolean classCompleted, boolean tutorCompletionConfirmed) {
        this.classId = classId;
        this.title = title;
        this.subjectName = subjectName;
        this.gradeName = gradeName;
        this.lessonMode = lessonMode;
        this.slots = slots;
        this.assignedTutorId = assignedTutorId;
        this.assignedTutorName = assignedTutorName;
        this.studentCount = studentCount;
        this.students = students;
        this.attendanceTaken = attendanceTaken;
        this.rescheduled = rescheduled;
        this.rescheduleNote = rescheduleNote;
        this.substituted = substituted;
        this.substituteNote = substituteNote;
        this.handedOff = handedOff;
        this.assistantTutorId = assistantTutorId;
        this.assistantTutorName = assistantTutorName;
        this.finalSession = finalSession;
        this.classCompleted = classCompleted;
        this.tutorCompletionConfirmed = tutorCompletionConfirmed;
    }

    public static CenterScheduleClassResponseBuilder builder() {
        return new CenterScheduleClassResponseBuilder();
    }

    public static class CenterScheduleClassResponseBuilder {
        private Long classId;
        private String title;
        private String subjectName;
        private String gradeName;
        private LessonMode lessonMode;
        private List<ScheduleSlotResponse> slots;
        private Long assignedTutorId;
        private String assignedTutorName;
        private int studentCount;
        private List<StudentAttendanceResponse> students;
        private boolean attendanceTaken;
        private boolean rescheduled;
        private String rescheduleNote;
        private boolean substituted;
        private String substituteNote;
        private boolean handedOff;
        private Long assistantTutorId;
        private String assistantTutorName;
        private boolean finalSession;
        private boolean classCompleted;
        private boolean tutorCompletionConfirmed;

        public CenterScheduleClassResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public CenterScheduleClassResponseBuilder title(String title) { this.title = title; return this; }
        public CenterScheduleClassResponseBuilder subjectName(String subjectName) { this.subjectName = subjectName; return this; }
        public CenterScheduleClassResponseBuilder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public CenterScheduleClassResponseBuilder lessonMode(LessonMode lessonMode) { this.lessonMode = lessonMode; return this; }
        public CenterScheduleClassResponseBuilder slots(List<ScheduleSlotResponse> slots) { this.slots = slots; return this; }
        public CenterScheduleClassResponseBuilder assignedTutorId(Long assignedTutorId) { this.assignedTutorId = assignedTutorId; return this; }
        public CenterScheduleClassResponseBuilder assignedTutorName(String assignedTutorName) { this.assignedTutorName = assignedTutorName; return this; }
        public CenterScheduleClassResponseBuilder studentCount(int studentCount) { this.studentCount = studentCount; return this; }
        public CenterScheduleClassResponseBuilder students(List<StudentAttendanceResponse> students) { this.students = students; return this; }
        public CenterScheduleClassResponseBuilder attendanceTaken(boolean attendanceTaken) { this.attendanceTaken = attendanceTaken; return this; }
        public CenterScheduleClassResponseBuilder rescheduled(boolean rescheduled) { this.rescheduled = rescheduled; return this; }
        public CenterScheduleClassResponseBuilder rescheduleNote(String rescheduleNote) { this.rescheduleNote = rescheduleNote; return this; }
        public CenterScheduleClassResponseBuilder substituted(boolean substituted) { this.substituted = substituted; return this; }
        public CenterScheduleClassResponseBuilder substituteNote(String substituteNote) { this.substituteNote = substituteNote; return this; }
        public CenterScheduleClassResponseBuilder handedOff(boolean handedOff) { this.handedOff = handedOff; return this; }
        public CenterScheduleClassResponseBuilder assistantTutorId(Long assistantTutorId) { this.assistantTutorId = assistantTutorId; return this; }
        public CenterScheduleClassResponseBuilder assistantTutorName(String assistantTutorName) { this.assistantTutorName = assistantTutorName; return this; }
        public CenterScheduleClassResponseBuilder finalSession(boolean finalSession) { this.finalSession = finalSession; return this; }
        public CenterScheduleClassResponseBuilder classCompleted(boolean classCompleted) { this.classCompleted = classCompleted; return this; }
        public CenterScheduleClassResponseBuilder tutorCompletionConfirmed(boolean tutorCompletionConfirmed) { this.tutorCompletionConfirmed = tutorCompletionConfirmed; return this; }

        public CenterScheduleClassResponse build() {
            return new CenterScheduleClassResponse(classId, title, subjectName, gradeName, lessonMode, slots, assignedTutorId, assignedTutorName, studentCount, students, attendanceTaken, rescheduled, rescheduleNote, substituted, substituteNote, handedOff, assistantTutorId, assistantTutorName, finalSession, classCompleted, tutorCompletionConfirmed);
        }
    }
}
