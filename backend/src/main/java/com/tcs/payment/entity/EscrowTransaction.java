package com.tcs.payment.entity;

import java.math.BigDecimal;

import com.tcs.marketplace.entity.ClassAssignment;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "escrow_transactions")
@Getter
@Setter
@NoArgsConstructor
public class EscrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "escrow_id", length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ClassAssignment assignment;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";
}
