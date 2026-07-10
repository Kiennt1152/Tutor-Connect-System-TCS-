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
    /** Địa chỉ thô (address_line) để đổ vào ô tự nhập khi chỉnh sửa. */
    private String locationText;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    private TutoringClassStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ScheduleSlotResponse> schedule;

    // Gia sư đang được gán dạy lớp (null nếu chưa gán).
    private Long assignedTutorId;
    private String assignedTutorName;

    // Danh sách học sinh đã ghi danh (ENROLLED) – dùng cho phần xem chi tiết.
    private List<StudentAttendanceResponse> students;
}
