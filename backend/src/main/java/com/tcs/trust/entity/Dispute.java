package com.tcs.trust.entity;

import com.tcs.payment.entity.EscrowTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dispute")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "dispute_id", length = 36, nullable = false, updatable = false)
    private UUID disputeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escrow_id", nullable = false)
    private EscrowTransaction escrow;

    @Column(name = "resolution", columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "Open";
}
