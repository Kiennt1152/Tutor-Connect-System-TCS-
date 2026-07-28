package com.tcs.module.marketplace.dto.response;

import com.tcs.module.marketplace.enums.ClassTerminationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClassTerminationResponse {

    private Long terminationId;

    private Long classId;

    private Long assignmentId;

    private Long classStudentId;

    private Long requestedByUserId;

    private String reason;

    private LocalDate effectiveDate;

    private ClassTerminationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
