package com.tcs.module.contract.dto.response;

import com.tcs.module.contract.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContractResponse {

    private Long contractId;
    private String contractNo;
    private ContractStatus status;
    private String termsSummary;
    private String contractFileUrl;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Class info
    private Long classId;
    private String classTitle;
    private String classType;
    private BigDecimal tuitionFee;
    private String lessonMode;
    private Integer numberOfSessions;

    // Parties
    private PartyInfo tutor;
    private PartyInfo client;
    private PartyInfo center;

    @Getter
    @Builder
    public static class PartyInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;
    }
}
