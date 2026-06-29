package com.tcs.module.platform.entity;

import com.tcs.module.identity.entity.User;
import com.tcs.module.platform.enums.UserPenaltyStatus;
import com.tcs.module.platform.enums.UserPenaltyType;
import com.tcs.module.profile.entity.PlatformAdmin;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_penalties")
@Getter
@Setter
@NoArgsConstructor
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id")
    private Long penaltyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private PlatformAdmin issuedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", length = 30, nullable = false)
    private UserPenaltyType penaltyType;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "evidence_urls", columnDefinition = "TEXT")
    private String evidenceUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "restriction_details", columnDefinition = "JSON")
    private String restrictionDetails;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private UserPenaltyStatus status = UserPenaltyStatus.ACTIVE;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", columnDefinition = "TEXT")
    private String revokedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
