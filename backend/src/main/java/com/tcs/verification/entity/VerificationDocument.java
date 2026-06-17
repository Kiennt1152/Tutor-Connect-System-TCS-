package com.tcs.verification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "verification_document")
@Getter
@Setter
@NoArgsConstructor
public class VerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "document_id", length = 36, nullable = false, updatable = false)
    private UUID documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private VerificationRequest verificationRequest;

    @Column(name = "file_url", length = 255, nullable = false)
    private String fileUrl;

    @Column(name = "document_type", length = 50, nullable = false)
    private String documentType;
}
