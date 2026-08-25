package com.tcs.module.center.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceResponse {

    private Long classStudentId;
    private String studentName;
    private String studentPhone;
    /** PRESENT / ABSENT / EXCUSED; null nếu chưa điểm danh buổi này. */
    private String status;

    public StudentAttendanceResponse() {}

    public StudentAttendanceResponse(Long classStudentId, String studentName, String studentPhone, String status) {
        this.classStudentId = classStudentId;
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.status = status;
    }

    public Long getClassStudentId() { return classStudentId; }
    public void setClassStudentId(Long classStudentId) { this.classStudentId = classStudentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentPhone() { return studentPhone; }
    public void setStudentPhone(String studentPhone) { this.studentPhone = studentPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static StudentAttendanceResponseBuilder builder() {
        return new StudentAttendanceResponseBuilder();
    }

    public static class StudentAttendanceResponseBuilder {
        private Long classStudentId;
        private String studentName;
        private String studentPhone;
        private String status;

        public StudentAttendanceResponseBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
        public StudentAttendanceResponseBuilder studentName(String studentName) { this.studentName = studentName; return this; }
        public StudentAttendanceResponseBuilder studentPhone(String studentPhone) { this.studentPhone = studentPhone; return this; }
        public StudentAttendanceResponseBuilder status(String status) { this.status = status; return this; }
        public StudentAttendanceResponse build() {
            return new StudentAttendanceResponse(classStudentId, studentName, studentPhone, status);
        }
    }
}
