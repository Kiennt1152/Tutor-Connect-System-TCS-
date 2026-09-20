package com.tcs.module.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Một khoảng thời gian gia sư tự khai báo là BẬN. Không khai báo = rảnh.
 *
 * <p>{@code startTime}/{@code endTime} cùng {@code null} nghĩa là bận cả ngày. Giờ kết thúc
 * {@code 00:00} là nửa đêm của chính ngày đó (xem {@link com.tcs.common.util.SlotTime}).
 */
@Entity
@Table(name = "tutor_busy_times")
@Getter
@Setter
@NoArgsConstructor
public class TutorBusyTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "busy_time_id")
    private Long busyTimeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "busy_date", nullable = false)
    private LocalDate busyDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "note", length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Bận cả ngày (không nêu khung giờ). */
    public boolean isAllDay() {
        return startTime == null && endTime == null;
    }
}
