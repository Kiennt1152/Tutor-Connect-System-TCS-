package com.tcs.module.contract.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TutorReputationResponse {

    private Long tutorId;
    private Long tutorUserId;
    private String fullName;
    private String avatar;
    private String bio;
    private Integer experienceYears;
    private BigDecimal hourlyRate;
    private String verificationStatus;

    private BigDecimal ratingAvg;
    private int totalReviews;

    private Map<Integer, Integer> ratingDistribution;

    private List<CriterionAverage> criteriaAverages;

    private List<ReviewResponse> reviews;

    @Getter
    @Builder
    public static class CriterionAverage {
        private String code;
        private String question;
        private BigDecimal average;
        private int count;
    }
}
