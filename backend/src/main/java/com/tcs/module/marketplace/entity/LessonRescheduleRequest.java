package com.tcs.module.marketplace.entity;

import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.enums.RescheduleRequestStatus;
import com.tcs.module.marketplace.enums.RescheduleRequestType;
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
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "lesson_reschedule_requests")
@Getter
@Setter
@NoArgsConstructor
public class LessonRescheduleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private TutoringClass tutoringClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", length = 20, nullable = false)
    private RescheduleRequestType requestType;

    @Column(name = "new_date", nullable = false)
    private LocalDate newDate;

    @Column(name = "new_start_time", nullable = false)
    private LocalTime newStartTime;

    @Column(name = "new_end_time", nullable = false)
    private LocalTime newEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RescheduleRequestStatus status = RescheduleRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
