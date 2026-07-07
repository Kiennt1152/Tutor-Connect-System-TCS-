package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewWithdrawalRequest {

    /** true = duyet (giai ngan), false = tu choi (hoan tien ve vi). */
    private boolean approve;

    /** Ly do (bat buoc khi tu choi). */
    private String reason;
}
