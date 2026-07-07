package com.tcs.module.finance.dto.response;

import com.tcs.module.finance.enums.PaymentTransactionStatus;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * Tra ve cho FE de hien thi ma QR VietQR va theo doi trang thai nap tien.
 * FE poll GET /api/finance/transactions cho toi khi status = SUCCESS.
 */
@Getter
@Builder
public class DepositResponse {

    private Long transactionId;
    private String referenceCode;
    private BigDecimal amount;
    private PaymentTransactionStatus status;

    /** Anh QR VietQR (img.vietqr.io) da nhung san amount + noi dung chuyen khoan. */
    private String qrImageUrl;

    private String bankName;
    private String accountNo;
    private String accountName;
    /** Noi dung chuyen khoan bat buoc (chinh la referenceCode). */
    private String transferContent;
}
