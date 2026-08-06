package com.tcs.module.finance.dto;

public record RefundPayoutInfo(
        String bankName,
        String accountNo,
        String accountHolderName) {
}
