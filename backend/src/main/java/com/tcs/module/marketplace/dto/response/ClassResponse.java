package com.tcs.module.marketplace.dto.response;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassResponse {

    private Long classId;
    private String title;
    private String description;
    private Long creatorId;
    private String creatorName;
    private Long subjectId;
    private String subjectName;
    private Long gradeId;
    private String gradeName;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private BigDecimal budget;
    private RecurringType recurringType;
    private TutoringClassStatus status;
    private LocalDateTime createdAt;
}
