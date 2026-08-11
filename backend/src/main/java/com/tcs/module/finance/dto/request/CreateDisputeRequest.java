package com.tcs.module.finance.dto.request;

import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.finance.dto.RefundPayoutInfo;
import com.tcs.module.platform.enums.ReportTargetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDisputeRequest {

    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private Long assignmentId;
    private Long classStudentId;
    private RefundPayoutInfo refundPayoutInfo;
}
