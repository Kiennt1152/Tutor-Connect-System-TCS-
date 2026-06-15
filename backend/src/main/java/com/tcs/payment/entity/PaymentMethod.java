package com.tcs.payment.entity;

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
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_method_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    @Column(name = "account_no", length = 50, nullable = false)
    private String accountNo;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";
}
