package com.tcs.module.ai.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TutorReferenceDto {
    Long tutorId;
    String fullName;
    String avatarUrl;
    String title;
    BigDecimal hourlyRate;
    Double averageRating;
    Integer totalReviews;
    String teachingAreas;
}
