package com.tcs.module.ai.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorReferenceDto {
    private Long tutorId;
    private String fullName;
    private String avatarUrl;
    private String title;
    private BigDecimal hourlyRate;
    private Double averageRating;
    private Integer totalReviews;
    private String teachingAreas;

    public Long getTutorId() { return tutorId; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getTitle() { return title; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public Double getAverageRating() { return averageRating; }
    public Integer getTotalReviews() { return totalReviews; }
    public String getTeachingAreas() { return teachingAreas; }
}
