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

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getRepresentativeName() { return representativeName; }
    public void setRepresentativeName(String representativeName) { this.representativeName = representativeName; }
    public String getRepresentativePosition() { return representativePosition; }
    public void setRepresentativePosition(String representativePosition) { this.representativePosition = representativePosition; }
}
