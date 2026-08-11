package com.tcs.module.finance.service;

import com.tcs.module.finance.dto.response.AdminEscrowPageResponse;
import com.tcs.module.finance.dto.response.AdminEscrowResponse;
import com.tcs.module.finance.enums.EscrowStatus;
import java.time.LocalDate;

public interface AdminEscrowService {
    AdminEscrowPageResponse search(EscrowStatus status, LocalDate from, LocalDate to, String reference,
            String payer, String beneficiary, int page, int size);
    AdminEscrowResponse get(Long escrowId);
}
