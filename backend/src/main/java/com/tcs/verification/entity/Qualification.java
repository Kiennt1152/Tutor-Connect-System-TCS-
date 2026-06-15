package com.tcs.verification.entity;

import java.time.LocalDate;

import com.tcs.user.entity.Tutor;
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
@Table(name = "qualifications")
@Getter
@Setter
@NoArgsConstructor
public class Qualification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "qualification_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "issuer", length = 150, nullable = false)
    private String issuer;

    @Column(name = "issue_date")
    private LocalDate issueDate;
}
