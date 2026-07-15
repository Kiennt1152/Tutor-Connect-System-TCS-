package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.platform.enums.ReportCategory;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DisputeResponse {

    private Long disputeId;
    private DisputeStatus disputeStatus;
    private Long reportId;
    private ReportStatus reportStatus;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportCategory category;
    private String description;
    private String evidenceUrls;
    private Long escrowId;
    private EscrowStatus escrowStatus;
    private LocalDateTime createdAt;
}
