package com.tcs.module.platform.dto.request;

import com.tcs.module.platform.enums.ReviewReportAction;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveReviewReportRequest {

    private ReviewReportAction action;
    private String notes;
}
