package com.tcs.module.marketplace.dto.request;

import com.tcs.module.marketplace.enums.LessonMode;
import com.tcs.module.marketplace.enums.RecurringType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateClassRequest {

    private String title;
    private String description;
    private Long categoryId;
    private Long subjectId;
    private Long gradeId;
    private String learningGoal;
    private String tutorRequirement;
    private Long locationId;
    private String address;
    private LessonMode lessonMode;
    private Integer numberOfSessions;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal tuitionFee;
    private BigDecimal budget;
    private RecurringType recurringType;
}
