package com.tcs.module.finance.entity;

import com.tcs.module.finance.enums.DisputeStatus;
import com.tcs.module.platform.entity.Report;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dispute_id")
    private Long disputeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escrow_id", nullable = false)
    private EscrowTransaction escrowTransaction;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private DisputeStatus status = DisputeStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
