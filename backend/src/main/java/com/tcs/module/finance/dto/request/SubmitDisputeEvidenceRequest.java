package com.tcs.module.finance.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitDisputeEvidenceRequest {

    private String evidenceUrls;
    private String note;

    public String getEvidenceUrls() { return evidenceUrls; }
    public void setEvidenceUrls(String evidenceUrls) { this.evidenceUrls = evidenceUrls; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
