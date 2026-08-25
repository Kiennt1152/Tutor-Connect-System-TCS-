package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveRefundPayoutRequest {

    private String bankName;
    private String accountNo;
    private String accountHolderName;

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
}
