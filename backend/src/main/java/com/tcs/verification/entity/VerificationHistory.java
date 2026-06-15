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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "verification_histories")
@Getter
@Setter
@NoArgsConstructor
public class VerificationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "verification_history_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private VerificationRequest verificationRequest;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20, nullable = false)
    private String newStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedByUser;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
