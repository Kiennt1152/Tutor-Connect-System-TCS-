package com.tcs.module.marketplace.entity;

import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.profile.entity.Tutor;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Long lessonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private TutoringClass tutoringClass;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ScheduleSlot slot;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "tutor_check_in_at")
    private LocalDateTime tutorCheckInAt;

    @Column(name = "tutor_check_out_at")
    private LocalDateTime tutorCheckOutAt;

    @Column(name = "client_confirm_at")
    private LocalDateTime clientConfirmAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", length = 20, nullable = false)
    private AttendanceStatus attendanceStatus = AttendanceStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm đã gửi thông báo nhắc nhở buổi học; null = chưa gửi (chống gửi trùng). */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }
    public TutoringClass getTutoringClass() { return tutoringClass; }
    public void setTutoringClass(TutoringClass tutoringClass) { this.tutoringClass = tutoringClass; }
    public LocalDate getLessonDate() { return lessonDate; }
    public void setLessonDate(LocalDate lessonDate) { this.lessonDate = lessonDate; }
    public ScheduleSlot getSlot() { return slot; }
    public void setSlot(ScheduleSlot slot) { this.slot = slot; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public Tutor getTutor() { return tutor; }
    public void setTutor(Tutor tutor) { this.tutor = tutor; }
    public LocalDateTime getTutorCheckInAt() { return tutorCheckInAt; }
    public void setTutorCheckInAt(LocalDateTime tutorCheckInAt) { this.tutorCheckInAt = tutorCheckInAt; }
    public LocalDateTime getTutorCheckOutAt() { return tutorCheckOutAt; }
    public void setTutorCheckOutAt(LocalDateTime tutorCheckOutAt) { this.tutorCheckOutAt = tutorCheckOutAt; }
    public LocalDateTime getClientConfirmAt() { return clientConfirmAt; }
    public void setClientConfirmAt(LocalDateTime clientConfirmAt) { this.clientConfirmAt = clientConfirmAt; }
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public User getApprovedByUser() { return approvedByUser; }
    public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(LocalDateTime reminderSentAt) { this.reminderSentAt = reminderSentAt; }
}
