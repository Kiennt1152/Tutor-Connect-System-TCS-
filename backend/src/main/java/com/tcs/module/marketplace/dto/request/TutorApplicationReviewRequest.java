package com.tcs.module.marketplace.dto.request;

import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TutorApplicationReviewRequest {

    /** ACCEPTED or REJECTED. UNDER_REVIEW/WITHDRAWN không dùng ở endpoint này. */
    private TutorApplicationStatus decision;
}