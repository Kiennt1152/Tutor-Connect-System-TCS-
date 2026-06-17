package com.tcs.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "class_assignment")
@Getter
@Setter
@NoArgsConstructor
public class ClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "assignment_id", length = 36, nullable = false, updatable = false)
    private UUID assignmentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private TutorApplication application;

    @CreationTimestamp
    @Column(name = "assigned_date", nullable = false, updatable = false)
    private LocalDateTime assignedDate;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";
}
