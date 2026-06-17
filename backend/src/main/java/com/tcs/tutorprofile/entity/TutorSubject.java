package com.tcs.tutorprofile.entity;

import com.tcs.catalog.entity.Subject;
import com.tcs.profile.entity.Tutor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tutor_subject",
        uniqueConstraints = @UniqueConstraint(name = "uq_tutor_subject", columnNames = {"tutor_id", "subject_id"}))
@Getter
@Setter
@NoArgsConstructor
public class TutorSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tutor_subject_id", length = 36, nullable = false, updatable = false)
    private UUID tutorSubjectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "proficiency_level", length = 50)
    private String proficiencyLevel;
}
