package com.tcs.module.marketplace.dto.response;

import com.tcs.module.marketplace.enums.ClassType;
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
public class ClassResponse {

    private Long classId;
    private String title;
    private String description;
    private String detailsJson;
    private Long creatorId;
    private String creatorName;
    private Long subjectId;
    private String subjectName;
    private Long gradeId;
    private String gradeName;
    private String learningGoal;
    private String tutorRequirement;
    private Long locationId;
    private String locationName;
    private String address;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private BigDecimal budget;
    private RecurringType recurringType;
    private TutoringClassStatus status;
    /** SELF/CENTER: lớp của trung tâm (CENTER) không cho gia sư tự đăng ký qua marketplace. */
    private ClassType classType;
    private Integer maxStudents;
    private long enrolledCount;
    private boolean canRequestTermination;
    private boolean refundAllowed;
    private String refundBlockedReason;
    private Integer totalSessions;
    private Integer completedSessions;
    private Long terminationAssignmentId;
    private Long terminationClassStudentId;
    /** UC "Xác nhận lớp đã hoàn thành" (chỉ lớp PRIVATE 1 gia sư – 1 phụ huynh/học viên). */
    private Long completionAssignmentId;
    /** True: người dùng hiện tại (gia sư hoặc phụ huynh/học viên) được phép bấm xác nhận hoàn thành. */
    private boolean canConfirmCompletion;
    /** True: người dùng hiện tại đã xác nhận, đang chờ bên còn lại xác nhận. */
    private boolean completionPendingOther;
    /** Lý do chưa cho xác nhận hoàn thành (vd: chưa điểm danh xong các buổi đã tới lịch). */
    private String completionBlockedReason;
    private List<ScheduleSlotResponse> schedule;
    private LocalDateTime createdAt;
    /** Hạn hiển thị (đăng lớp + 30 ngày); null nếu không tính hạn. Chỉ có với lớp OPEN. */
    private LocalDateTime expiresAt;
    private Long applicationCount;
    private Long assignmentId;
}
