package com.tcs.module.center.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Trung tâm nhập thông tin bổ sung cho khối BÊN A của hợp đồng. */
@Getter
@Setter
public class SaveCenterContractInfoRequest {
    private String website;
    private String representativeName;
    private String representativePosition;
}
