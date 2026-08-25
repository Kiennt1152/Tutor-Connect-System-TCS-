package com.tcs.module.center.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Thống kê tình trạng lớp học của trung tâm: điểm danh (có mặt/vắng/có phép) theo lớp, giáo viên, học sinh. */
@Getter
@Builder
public class CenterStatsResponse {

    private Totals totals;
    private List<ClassStat> classes;
    private List<StudentStat> students;

    @Getter
    @Builder
    public static class Totals {
        private int classCount;
        private int activeClassCount;
        private int completedClassCount;
        private int studentCount;
        private long present;
        private long absent;
        private long excused;
        private long totalMarks;      // tổng lượt điểm danh (present + absent + excused)
        private double attendanceRate; // % có mặt trên tổng lượt điểm danh
    }

    @Getter
    @Builder
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
    }

    @Getter
    @Builder
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
    }
}
