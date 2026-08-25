package com.tcs.module.center.dto.request;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** Trung tâm duyệt/từ chối một yêu cầu dời buổi học. */
@Getter
@Setter
public class RescheduleDecisionBody {

    private Long classId;
    private LocalDate originalDate;
    private boolean approve;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public LocalDate getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDate originalDate) { this.originalDate = originalDate; }
    public boolean isApprove() { return approve; }
    public void setApprove(boolean approve) { this.approve = approve; }
}
