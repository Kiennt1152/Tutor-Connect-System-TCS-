package com.tcs.module.contract.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveRefundPayoutRequest {

    private String bankName;

    private String accountNo;

    private String accountHolderName;
}
