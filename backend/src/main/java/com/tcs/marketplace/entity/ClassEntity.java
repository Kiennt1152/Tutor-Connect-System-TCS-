package com.tcs.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.tcs.masterdata.entity.Grade;
import com.tcs.masterdata.entity.Location;
import com.tcs.masterdata.entity.Subject;
import com.tcs.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "classes")
@Getter
@Setter
@NoArgsConstructor
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @Column(name = "max_sessions", nullable = false)
    private Integer maxSessions = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget", precision = 12, scale = 2, nullable = false)
    private BigDecimal budget = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "recurring_type", length = 20, nullable = false)
    private String recurringType = "ONCE";
}
