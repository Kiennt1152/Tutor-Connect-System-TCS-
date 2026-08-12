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
}
