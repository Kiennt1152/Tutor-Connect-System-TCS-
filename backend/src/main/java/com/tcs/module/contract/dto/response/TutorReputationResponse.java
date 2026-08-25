package com.tcs.module.contract.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public TutorReputationResponse() {}

    public TutorReputationResponse(Long tutorId, Long tutorUserId, String fullName, String avatar, String bio, Integer experienceYears, BigDecimal hourlyRate, String verificationStatus, BigDecimal ratingAvg, int totalReviews, Map<Integer, Integer> ratingDistribution, List<CriterionAverage> criteriaAverages, List<ReviewResponse> reviews) {
        this.tutorId = tutorId;
        this.tutorUserId = tutorUserId;
        this.fullName = fullName;
        this.avatar = avatar;
        this.bio = bio;
        this.experienceYears = experienceYears;
        this.hourlyRate = hourlyRate;
        this.verificationStatus = verificationStatus;
        this.ratingAvg = ratingAvg;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
        this.criteriaAverages = criteriaAverages;
        this.reviews = reviews;
    }

    public static TutorReputationResponseBuilder builder() {
        return new TutorReputationResponseBuilder();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriterionAverage {
        private String code;
        private String question;
        private BigDecimal average;
        private int count;

        public CriterionAverage() {}

        public CriterionAverage(String code, String question, BigDecimal average, int count) {
            this.code = code;
            this.question = question;
            this.average = average;
            this.count = count;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public BigDecimal getAverage() { return average; }
        public void setAverage(BigDecimal average) { this.average = average; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public static CriterionAverageBuilder builder() { return new CriterionAverageBuilder(); }

        public static class CriterionAverageBuilder {
            private String code;
            private String question;
            private BigDecimal average;
            private int count;

            public CriterionAverageBuilder code(String code) { this.code = code; return this; }
            public CriterionAverageBuilder question(String question) { this.question = question; return this; }
            public CriterionAverageBuilder average(BigDecimal average) { this.average = average; return this; }
            public CriterionAverageBuilder count(int count) { this.count = count; return this; }
            public CriterionAverage build() {
                return new CriterionAverage(code, question, average, count);
            }
        }
    }

    public static class TutorReputationResponseBuilder {
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

        public TutorReputationResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public TutorReputationResponseBuilder tutorUserId(Long tutorUserId) { this.tutorUserId = tutorUserId; return this; }
        public TutorReputationResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public TutorReputationResponseBuilder avatar(String avatar) { this.avatar = avatar; return this; }
        public TutorReputationResponseBuilder bio(String bio) { this.bio = bio; return this; }
        public TutorReputationResponseBuilder experienceYears(Integer experienceYears) { this.experienceYears = experienceYears; return this; }
        public TutorReputationResponseBuilder hourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; return this; }
        public TutorReputationResponseBuilder verificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; return this; }
        public TutorReputationResponseBuilder ratingAvg(BigDecimal ratingAvg) { this.ratingAvg = ratingAvg; return this; }
        public TutorReputationResponseBuilder totalReviews(int totalReviews) { this.totalReviews = totalReviews; return this; }
        public TutorReputationResponseBuilder ratingDistribution(Map<Integer, Integer> ratingDistribution) { this.ratingDistribution = ratingDistribution; return this; }
        public TutorReputationResponseBuilder criteriaAverages(List<CriterionAverage> criteriaAverages) { this.criteriaAverages = criteriaAverages; return this; }
        public TutorReputationResponseBuilder reviews(List<ReviewResponse> reviews) { this.reviews = reviews; return this; }

        public TutorReputationResponse build() {
            return new TutorReputationResponse(tutorId, tutorUserId, fullName, avatar, bio, experienceYears, hourlyRate, verificationStatus, ratingAvg, totalReviews, ratingDistribution, criteriaAverages, reviews);
        }
    }
}
