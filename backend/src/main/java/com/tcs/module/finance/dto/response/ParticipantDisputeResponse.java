package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.DisputeStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantDisputeResponse {
    private Long disputeId;
    private Long classId;
    private String classTitle;
    private DisputeStatus status;
    private String description;
    private List<String> evidenceUrls;
    private String resolution;
    private LocalDateTime createdAt;
    private boolean canRespond;
    private boolean canWithdraw;
    private String withdrawalBlockedReason;
    private List<Update> updates;

    public record Update(String author, String note, LocalDateTime createdAt) {}
}
