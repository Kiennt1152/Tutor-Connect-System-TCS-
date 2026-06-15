package com.tcs.marketplace.entity;

import java.time.LocalDateTime;

import com.tcs.user.entity.Tutor;
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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tutor_applications")
@Getter
@Setter
@NoArgsConstructor
public class TutorApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "application_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "SUBMITTED";

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;
}
