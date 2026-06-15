package com.tcs.payment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tcs.marketplace.entity.ClassAssignment;
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
@Table(name = "payment_release_requests")
@Getter
@Setter
@NoArgsConstructor
public class PaymentReleaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ClassAssignment assignment;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;
}
