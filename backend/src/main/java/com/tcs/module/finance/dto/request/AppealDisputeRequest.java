package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppealDisputeRequest {

    private String reason;
    private String evidenceUrls;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }
}
