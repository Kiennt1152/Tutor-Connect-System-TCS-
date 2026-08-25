package com.tcs.module.center.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một yêu cầu nhờ gia sư phụ dạy thay để hiển thị cho gia sư và trung tâm. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubstitutionResponse {

    private Long classId;
    private String className;
    private LocalDate date;
    private String status; // PENDING | APPROVED | REJECTED
    private String reason;
    /** Gia sư chính (người xin dạy thay). */
    private Long mainTutorId;
    private String mainTutorName;
    /** Gia sư phụ được nhờ dạy thay. */
    private Long assistantTutorId;
    private String assistantTutorName;

    public SubstitutionResponse() {}

    public SubstitutionResponse(Long classId, String className, LocalDate date, String status, String reason, Long mainTutorId, String mainTutorName, Long assistantTutorId, String assistantTutorName) {
        this.classId = classId;
        this.className = className;
        this.date = date;
        this.status = status;
        this.reason = reason;
        this.mainTutorId = mainTutorId;
        this.mainTutorName = mainTutorName;
        this.assistantTutorId = assistantTutorId;
        this.assistantTutorName = assistantTutorName;
    }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getMainTutorId() { return mainTutorId; }
    public void setMainTutorId(Long mainTutorId) { this.mainTutorId = mainTutorId; }
    public String getMainTutorName() { return mainTutorName; }
    public void setMainTutorName(String mainTutorName) { this.mainTutorName = mainTutorName; }
    public Long getAssistantTutorId() { return assistantTutorId; }
    public void setAssistantTutorId(Long assistantTutorId) { this.assistantTutorId = assistantTutorId; }
    public String getAssistantTutorName() { return assistantTutorName; }
    public void setAssistantTutorName(String assistantTutorName) { this.assistantTutorName = assistantTutorName; }

    public static SubstitutionResponseBuilder builder() {
        return new SubstitutionResponseBuilder();
    }

    public static class SubstitutionResponseBuilder {
        private Long classId;
        private String className;
        private LocalDate date;
        private String status;
        private String reason;
        private Long mainTutorId;
        private String mainTutorName;
        private Long assistantTutorId;
        private String assistantTutorName;

        public SubstitutionResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public SubstitutionResponseBuilder className(String className) { this.className = className; return this; }
        public SubstitutionResponseBuilder date(LocalDate date) { this.date = date; return this; }
        public SubstitutionResponseBuilder status(String status) { this.status = status; return this; }
        public SubstitutionResponseBuilder reason(String reason) { this.reason = reason; return this; }
        public SubstitutionResponseBuilder mainTutorId(Long mainTutorId) { this.mainTutorId = mainTutorId; return this; }
        public SubstitutionResponseBuilder mainTutorName(String mainTutorName) { this.mainTutorName = mainTutorName; return this; }
        public SubstitutionResponseBuilder assistantTutorId(Long assistantTutorId) { this.assistantTutorId = assistantTutorId; return this; }
        public SubstitutionResponseBuilder assistantTutorName(String assistantTutorName) { this.assistantTutorName = assistantTutorName; return this; }
        public SubstitutionResponse build() {
            return new SubstitutionResponse(classId, className, date, status, reason, mainTutorId, mainTutorName, assistantTutorId, assistantTutorName);
        }
    }
}
