package com.tcs.module.marketplace.mapper;

import com.tcs.module.marketplace.dto.response.TutorApplicationResponse;
import com.tcs.module.marketplace.entity.TutorApplication;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceMapper {

    public TutorApplicationResponse toResponse(TutorApplication application) {
        var tutor = application.getTutor();
        var tutoringClass = application.getTutoringClass();
        return TutorApplicationResponse.builder()
                .applicationId(application.getApplicationId())
                .classId(tutoringClass.getClassId())
                .classTitle(tutoringClass.getTitle())
                .tutorId(tutor.getTutorId())
                .tutorName(tutor.getFullName())
                .tutorAvatarUrl(tutor.getAvatar())
                .tutorRatingAvg(tutor.getRatingAvg())
                .tutorVerificationStatus(
                        tutor.getVerificationStatus() != null ? tutor.getVerificationStatus().name() : null)
                .proposedRate(application.getProposedRate())
                .coverLetter(application.getCoverLetter())
                .status(application.getStatus())
                .appliedAt(application.getAppliedAt())
                .reviewedAt(application.getReviewedAt())
                .build();
    }

    public List<TutorApplicationResponse> toResponseList(List<TutorApplication> applications) {
        return applications.stream().map(this::toResponse).toList();
    }
}