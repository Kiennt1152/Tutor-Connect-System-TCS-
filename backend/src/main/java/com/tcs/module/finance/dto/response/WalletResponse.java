package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.WalletStatus;
import com.tcs.module.profile.enums.GuardianApprovalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {

    private Long walletId;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;
    private WalletStatus status;
    private LocalDateTime updatedAt;

    private Long legalOwnerUserId;
    private String legalOwnerName;
    private boolean delegatedToParent;
    private Long beneficiaryMinorUserId;
    private String beneficiaryMinorName;

    /** Thao tác vừa thực hiện đang chờ phụ huynh xác nhận. */
    private boolean pendingGuardianApproval;
    private Long guardianApprovalId;
    private GuardianApprovalStatus guardianApprovalStatus;
    private String message;
}
