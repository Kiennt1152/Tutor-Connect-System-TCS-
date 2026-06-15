package com.tcs.verification.entity;

import java.time.LocalDateTime;

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
@Table(name = "verification_requests")
@Getter
@Setter
@NoArgsConstructor
public class VerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "verification_type", length = 50, nullable = false)
    private String verificationType;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
