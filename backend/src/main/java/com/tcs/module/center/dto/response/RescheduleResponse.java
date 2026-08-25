package com.tcs.module.center.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Một yêu cầu dời buổi học để hiển thị cho gia sư và trung tâm. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleResponse {

    private Long classId;
    private String className;
    private LocalDate originalDate;
    private LocalDate newDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime newStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime newEndTime;

    private String status; // PENDING | APPROVED | REJECTED
    private Long tutorId;
    private String tutorName;
    private String reason;

    public RescheduleResponse() {}

    public RescheduleResponse(Long classId, String className, LocalDate originalDate, LocalDate newDate, LocalTime newStartTime, LocalTime newEndTime, String status, Long tutorId, String tutorName, String reason) {
        this.classId = classId;
        this.className = className;
        this.originalDate = originalDate;
        this.newDate = newDate;
        this.newStartTime = newStartTime;
        this.newEndTime = newEndTime;
        this.status = status;
        this.tutorId = tutorId;
        this.tutorName = tutorName;
        this.reason = reason;
    }

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public LocalDate getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDate originalDate) { this.originalDate = originalDate; }
    public LocalDate getNewDate() { return newDate; }
    public void setNewDate(LocalDate newDate) { this.newDate = newDate; }
    public LocalTime getNewStartTime() { return newStartTime; }
    public void setNewStartTime(LocalTime newStartTime) { this.newStartTime = newStartTime; }
    public LocalTime getNewEndTime() { return newEndTime; }
    public void setNewEndTime(LocalTime newEndTime) { this.newEndTime = newEndTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTutorId() { return tutorId; }
    public void setTutorId(Long tutorId) { this.tutorId = tutorId; }
    public String getTutorName() { return tutorName; }
    public void setTutorName(String tutorName) { this.tutorName = tutorName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public static RescheduleResponseBuilder builder() {
        return new RescheduleResponseBuilder();
    }

    public static class RescheduleResponseBuilder {
        private Long classId;
        private String className;
        private LocalDate originalDate;
        private LocalDate newDate;
        private LocalTime newStartTime;
        private LocalTime newEndTime;
        private String status;
        private Long tutorId;
        private String tutorName;
        private String reason;

        public RescheduleResponseBuilder classId(Long classId) { this.classId = classId; return this; }
        public RescheduleResponseBuilder className(String className) { this.className = className; return this; }
        public RescheduleResponseBuilder originalDate(LocalDate originalDate) { this.originalDate = originalDate; return this; }
        public RescheduleResponseBuilder newDate(LocalDate newDate) { this.newDate = newDate; return this; }
        public RescheduleResponseBuilder newStartTime(LocalTime newStartTime) { this.newStartTime = newStartTime; return this; }
        public RescheduleResponseBuilder newEndTime(LocalTime newEndTime) { this.newEndTime = newEndTime; return this; }
        public RescheduleResponseBuilder status(String status) { this.status = status; return this; }
        public RescheduleResponseBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
        public RescheduleResponseBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
        public RescheduleResponseBuilder reason(String reason) { this.reason = reason; return this; }
        public RescheduleResponse build() {
            return new RescheduleResponse(classId, className, originalDate, newDate, newStartTime, newEndTime, status, tutorId, tutorName, reason);
        }
    }
}
