package com.tcs.module.finance.util;

import com.tcs.module.finance.dto.RefundPayoutInfo;

public final class RefundPayoutInfoCodec {

    public static final String PAYOUT_HEADER = "Thông tin nhận hoàn tiền:";
    private static final String ACCOUNT_HOLDER_LABEL = "Tên chủ tài khoản:";
    private static final String BANK_LABEL = "Ngân hàng:";
    private static final String ACCOUNT_NO_LABEL = "Số tài khoản:";

    private RefundPayoutInfoCodec() {
    }

    public static String appendToReason(String reason, RefundPayoutInfo payoutInfo) {
        String cleanReason = stripFromReason(reason);
        if (!hasCompletePayout(payoutInfo)) {
            return cleanReason;
        }
        String base = cleanReason == null || cleanReason.isBlank() ? "" : cleanReason.trim() + "\n\n";
        return base
                + PAYOUT_HEADER + "\n"
                + "- " + ACCOUNT_HOLDER_LABEL + " " + normalize(payoutInfo.accountHolderName()) + "\n"
                + "- " + BANK_LABEL + " " + normalize(payoutInfo.bankName()) + "\n"
                + "- " + ACCOUNT_NO_LABEL + " " + normalizeAccountNo(payoutInfo.accountNo());
    }

    public static String stripFromReason(String reason) {
        if (reason == null) {
            return null;
        }
        int index = reason.indexOf(PAYOUT_HEADER);
        if (index < 0) {
            return reason.trim();
        }
        return reason.substring(0, index).trim();
    }

    public static RefundPayoutInfo parseFromReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String bankName = null;
        String accountNo = null;
        String accountHolderName = null;

        for (String rawLine : reason.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("-")) {
                line = line.substring(1).trim();
            }
            if (line.startsWith(ACCOUNT_HOLDER_LABEL)) {
                accountHolderName = normalize(line.substring(ACCOUNT_HOLDER_LABEL.length()));
            } else if (line.startsWith(BANK_LABEL)) {
                bankName = normalize(line.substring(BANK_LABEL.length()));
            } else if (line.startsWith(ACCOUNT_NO_LABEL)) {
                accountNo = normalizeAccountNo(line.substring(ACCOUNT_NO_LABEL.length()));
            }
        }

        if (isBlank(bankName) && isBlank(accountNo) && isBlank(accountHolderName)) {
            return null;
        }
        return new RefundPayoutInfo(bankName, accountNo, accountHolderName);
    }

    public static boolean hasCompletePayout(RefundPayoutInfo payoutInfo) {
        return payoutInfo != null
                && !isBlank(payoutInfo.bankName())
                && !isBlank(payoutInfo.accountNo())
                && !isBlank(payoutInfo.accountHolderName());
    }

    public static String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) {
            return null;
        }
        String normalized = normalizeAccountNo(accountNo);
        if (normalized.length() <= 4) {
            return "****" + normalized;
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    public static String normalizeAccountNo(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
