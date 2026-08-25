package com.tcs.module.identity.entity;

import com.tcs.module.identity.enums.VerificationDocumentType;
import com.tcs.module.profile.entity.MediaFile;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "verification_documents")
@Getter
@Setter
@NoArgsConstructor
public class VerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_id", nullable = false)
    private VerificationRequest verificationRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private MediaFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50, nullable = false)
    private VerificationDocumentType documentType;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public VerificationRequest getVerificationRequest() { return verificationRequest; }
    public void setVerificationRequest(VerificationRequest verificationRequest) { this.verificationRequest = verificationRequest; }
    public MediaFile getFile() { return file; }
    public void setFile(MediaFile file) { this.file = file; }
    public VerificationDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(VerificationDocumentType documentType) { this.documentType = documentType; }
}
