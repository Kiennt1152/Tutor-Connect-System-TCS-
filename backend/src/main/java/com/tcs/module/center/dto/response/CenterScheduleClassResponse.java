package com.tcs.module.center.dto.response;

import com.tcs.module.marketplace.enums.LessonMode;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Một lớp có buổi học trong ngày, kèm gia sư, danh sách học sinh và điểm danh. */
@Getter
@Setter
@Builder
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
}
