package com.tcs.module.center.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thống kê tình trạng lớp học của trung tâm: điểm danh (có mặt/vắng/có phép) theo lớp, giáo viên, học sinh. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterStatsResponse {

    private Totals totals;
    private List<ClassStat> classes;
    private List<StudentStat> students;

    public CenterStatsResponse() {}

    public CenterStatsResponse(Totals totals, List<ClassStat> classes, List<StudentStat> students) {
        this.totals = totals;
        this.classes = classes;
        this.students = students;
    }

    public Totals getTotals() { return totals; }
    public void setTotals(Totals totals) { this.totals = totals; }
    public List<ClassStat> getClasses() { return classes; }
    public void setClasses(List<ClassStat> classes) { this.classes = classes; }
    public List<StudentStat> getStudents() { return students; }
    public void setStudents(List<StudentStat> students) { this.students = students; }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Totals {
        private int classCount;
        private int activeClassCount;
        private int completedClassCount;
        private int studentCount;
        private long present;
        private long absent;
        private long excused;
        private long totalMarks;
        private double attendanceRate;

        public Totals() {}

        public Totals(int classCount, int activeClassCount, int completedClassCount, int studentCount, long present, long absent, long excused, long totalMarks, double attendanceRate) {
            this.classCount = classCount;
            this.activeClassCount = activeClassCount;
            this.completedClassCount = completedClassCount;
            this.studentCount = studentCount;
            this.present = present;
            this.absent = absent;
            this.excused = excused;
            this.totalMarks = totalMarks;
            this.attendanceRate = attendanceRate;
        }

        public int getClassCount() { return classCount; }
        public int getActiveClassCount() { return activeClassCount; }
        public int getCompletedClassCount() { return completedClassCount; }
        public int getStudentCount() { return studentCount; }
        public long getPresent() { return present; }
        public long getAbsent() { return absent; }
        public long getExcused() { return excused; }
        public long totalMarks() { return totalMarks; }
        public long getTotalMarks() { return totalMarks; }
        public double getAttendanceRate() { return attendanceRate; }

        public static TotalsBuilder builder() { return new TotalsBuilder(); }

        public static class TotalsBuilder {
            private int classCount;
            private int activeClassCount;
            private int completedClassCount;
            private int studentCount;
            private long present;
            private long absent;
            private long excused;
            private long totalMarks;
            private double attendanceRate;

            public TotalsBuilder classCount(int classCount) { this.classCount = classCount; return this; }
            public TotalsBuilder activeClassCount(int activeClassCount) { this.activeClassCount = activeClassCount; return this; }
            public TotalsBuilder completedClassCount(int completedClassCount) { this.completedClassCount = completedClassCount; return this; }
            public TotalsBuilder studentCount(int studentCount) { this.studentCount = studentCount; return this; }
            public TotalsBuilder present(long present) { this.present = present; return this; }
            public TotalsBuilder absent(long absent) { this.absent = absent; return this; }
            public TotalsBuilder excused(long excused) { this.excused = excused; return this; }
            public TotalsBuilder totalMarks(long totalMarks) { this.totalMarks = totalMarks; return this; }
            public TotalsBuilder attendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; return this; }
            public Totals build() {
                return new Totals(classCount, activeClassCount, completedClassCount, studentCount, present, absent, excused, totalMarks, attendanceRate);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassStat {
        private Long classId;
        private String title;
        private String status;
        private Long tutorId;
        private String tutorName;
        private int studentCount;
        private long present;
        private long absent;
        private long excused;
        private double attendanceRate;

        public ClassStat() {}

        public ClassStat(Long classId, String title, String status, Long tutorId, String tutorName, int studentCount, long present, long absent, long excused, double attendanceRate) {
            this.classId = classId;
            this.title = title;
            this.status = status;
            this.tutorId = tutorId;
            this.tutorName = tutorName;
            this.studentCount = studentCount;
            this.present = present;
            this.absent = absent;
            this.excused = excused;
            this.attendanceRate = attendanceRate;
        }

        public Long getClassId() { return classId; }
        public String getTitle() { return title; }
        public String getStatus() { return status; }
        public Long getTutorId() { return tutorId; }
        public String getTutorName() { return tutorName; }
        public int getStudentCount() { return studentCount; }
        public long getPresent() { return present; }
        public long getAbsent() { return absent; }
        public long getExcused() { return excused; }
        public double getAttendanceRate() { return attendanceRate; }

        public static ClassStatBuilder builder() { return new ClassStatBuilder(); }

        public static class ClassStatBuilder {
            private Long classId;
            private String title;
            private String status;
            private Long tutorId;
            private String tutorName;
            private int studentCount;
            private long present;
            private long absent;
            private long excused;
            private double attendanceRate;

            public ClassStatBuilder classId(Long classId) { this.classId = classId; return this; }
            public ClassStatBuilder title(String title) { this.title = title; return this; }
            public ClassStatBuilder status(String status) { this.status = status; return this; }
            public ClassStatBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
            public ClassStatBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
            public ClassStatBuilder studentCount(int studentCount) { this.studentCount = studentCount; return this; }
            public ClassStatBuilder present(long present) { this.present = present; return this; }
            public ClassStatBuilder absent(long absent) { this.absent = absent; return this; }
            public ClassStatBuilder excused(long excused) { this.excused = excused; return this; }
            public ClassStatBuilder attendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; return this; }
            public ClassStat build() {
                return new ClassStat(classId, title, status, tutorId, tutorName, studentCount, present, absent, excused, attendanceRate);
            }
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStat {
        private Long classStudentId;
        private String studentName;
        private Long classId;
        private String className;
        private Long tutorId;
        private String tutorName;
        private long present;
        private long absent;
        private long excused;
        private double attendanceRate;

        public StudentStat() {}

        public StudentStat(Long classStudentId, String studentName, Long classId, String className, Long tutorId, String tutorName, long present, long absent, long excused, double attendanceRate) {
            this.classStudentId = classStudentId;
            this.studentName = studentName;
            this.classId = classId;
            this.className = className;
            this.tutorId = tutorId;
            this.tutorName = tutorName;
            this.present = present;
            this.absent = absent;
            this.excused = excused;
            this.attendanceRate = attendanceRate;
        }

        public Long getClassStudentId() { return classStudentId; }
        public String getStudentName() { return studentName; }
        public Long getClassId() { return classId; }
        public String getClassName() { return className; }
        public Long getTutorId() { return tutorId; }
        public String getTutorName() { return tutorName; }
        public long getPresent() { return present; }
        public long getAbsent() { return absent; }
        public long getExcused() { return excused; }
        public double getAttendanceRate() { return attendanceRate; }

        public static StudentStatBuilder builder() { return new StudentStatBuilder(); }

        public static class StudentStatBuilder {
            private Long classStudentId;
            private String studentName;
            private Long classId;
            private String className;
            private Long tutorId;
            private String tutorName;
            private long present;
            private long absent;
            private long excused;
            private double attendanceRate;

            public StudentStatBuilder classStudentId(Long classStudentId) { this.classStudentId = classStudentId; return this; }
            public StudentStatBuilder studentName(String studentName) { this.studentName = studentName; return this; }
            public StudentStatBuilder classId(Long classId) { this.classId = classId; return this; }
            public StudentStatBuilder className(String className) { this.className = className; return this; }
            public StudentStatBuilder tutorId(Long tutorId) { this.tutorId = tutorId; return this; }
            public StudentStatBuilder tutorName(String tutorName) { this.tutorName = tutorName; return this; }
            public StudentStatBuilder present(long present) { this.present = present; return this; }
            public StudentStatBuilder absent(long absent) { this.absent = absent; return this; }
            public StudentStatBuilder excused(long excused) { this.excused = excused; return this; }
            public StudentStatBuilder attendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; return this; }
            public StudentStat build() {
                return new StudentStat(classStudentId, studentName, classId, className, tutorId, tutorName, present, absent, excused, attendanceRate);
            }
        }
    }

    public static CenterStatsResponseBuilder builder() { return new CenterStatsResponseBuilder(); }

    public static class CenterStatsResponseBuilder {
        private Totals totals;
        private List<ClassStat> classes;
        private List<StudentStat> students;

        public CenterStatsResponseBuilder totals(Totals totals) { this.totals = totals; return this; }
        public CenterStatsResponseBuilder classes(List<ClassStat> classes) { this.classes = classes; return this; }
        public CenterStatsResponseBuilder students(List<StudentStat> students) { this.students = students; return this; }
        public CenterStatsResponse build() {
            return new CenterStatsResponse(totals, classes, students);
        }
    }
}
