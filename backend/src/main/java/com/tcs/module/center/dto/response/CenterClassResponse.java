package com.tcs.module.center.dto.response;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CenterClassResponse {

    private Long classId;
    private String title;
    private String description;
    private Long creatorId;
    private Long centerId;
    private Long categoryId;
    private String categoryName;
    private Long subjectId;
    private String subjectName;
    private Long gradeId;
    private String gradeName;
    private Long locationId;
    private String locationLabel;
    /** Địa chỉ thô (address_line) để hiển thị. */
    private String locationText;
    /** Địa điểm 2 cấp + địa chỉ cụ thể, để đổ lại form khi chỉnh sửa. */
    private String provinceName;
    private String wardName;
    private String addressDetail;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    private Integer minStudents;
    /** Số học sinh đã ghi danh (ENROLLED) — để hiển thị tiến độ & điều kiện kích hoạt. */
    private long enrolledCount;
    /** Loại lớp: EXTERNAL (yêu cầu ngoài) / SELF (tự tạo). */
    private String originType;
    /** Mẫu hợp đồng học viên đã chọn cho lớp (để đổ lại form khi sửa). */
    private Long contractTemplateId;
    /** Nội dung điều khoản HĐ học viên đã lưu cho lớp (để đổ lại form khi sửa). */
    private String contractContent;
    private TutoringClassStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScheduleSlotResponse> schedule;

    // Gia sư đang được gán dạy lớp (null nếu chưa gán).
    private Long assignedTutorId;
    private String assignedTutorName;

    // Gia sư phụ (backup) của lớp, dùng để dạy thay khi gia sư chính báo ốm/bận (null nếu chưa gán).
    private Long assistantTutorId;
    private String assistantTutorName;

    // Danh sách học sinh đã ghi danh (ENROLLED) – dùng cho phần xem chi tiết.
    private List<StudentAttendanceResponse> students;

    /** true nếu GIA SƯ đã xác nhận hoàn thành — trung tâm cần xác nhận để đóng lớp. */
    private boolean tutorCompletionConfirmed;
}
