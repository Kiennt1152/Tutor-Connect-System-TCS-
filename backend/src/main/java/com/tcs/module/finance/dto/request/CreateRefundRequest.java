package com.tcs.module.finance.dto.request;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRefundRequest {

    private Long escrowId;
    private Long assignmentId;
    private Long classStudentId;
    private BigDecimal amount;
    private String reason;
}
