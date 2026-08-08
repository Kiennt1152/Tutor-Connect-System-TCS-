package com.tcs.module.marketplace.entity;

import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "class_assignments")
@Getter
@Setter
@NoArgsConstructor
public class ClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long assignmentId;

    // Nguon su that cua tutor. Voi gan noi bo (INTERNAL) chi co tutor,
    // voi tuyen qua marketplace (EXTERNAL) co them application.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    // Nullable: chi set khi tutor duoc chon qua don ung tuyen (EXTERNAL).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", unique = true)
    private TutorApplication application;

    @CreationTimestamp
    @Column(name = "assigned_date", nullable = false, updatable = false)
    private LocalDateTime assignedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ClassAssignmentStatus status = ClassAssignmentStatus.ACTIVE;

    @Column(name = "tutor_signed_at")
    private LocalDateTime tutorSignedAt;

    @Column(name = "client_signed_at")
    private LocalDateTime clientSignedAt;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "terms_b", columnDefinition = "TEXT")
    private String termsB;
}
