package com.tcs.module.marketplace.entity;

import com.tcs.module.marketplace.enums.LessonAttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Diem danh theo tung hoc sinh cho moi buoi hoc (BF-06 Center Course Delivery).
 * Lop CENTER dung bang nay; lop PRIVATE van dung lessons.attendance_status.
 */
@Entity
@Table(name = "lesson_attendances")
@Getter
@Setter
@NoArgsConstructor
public class LessonAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_student_id", nullable = false)
    private ClassStudent classStudent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LessonAttendanceStatus status = LessonAttendanceStatus.PRESENT;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
