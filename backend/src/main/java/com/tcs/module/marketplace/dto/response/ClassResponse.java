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
    private List<ScheduleSlotResponse> schedule;
    private LocalDateTime createdAt;
    private Long applicationCount;
}
