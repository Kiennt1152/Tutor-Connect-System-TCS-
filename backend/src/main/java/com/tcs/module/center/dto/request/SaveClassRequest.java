package com.tcs.module.center.dto.request;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Dùng chung cho tạo mới và chỉnh sửa lớp học của Trung tâm gia sư (UC-14-B). */
@Getter
@Setter
public class SaveClassRequest {

    private String title;
    private String description;
    // Người dùng tự nhập tên (backend sẽ tìm-hoặc-tạo bản ghi danh mục tương ứng).
    private String categoryName;
    private String subjectName;
    private String gradeName;
    private String locationText;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private RecurringType recurringType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private Integer maxStudents;
    private List<ScheduleSlotRequest> schedule;
}
