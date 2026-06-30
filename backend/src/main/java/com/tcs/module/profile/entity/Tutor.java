package com.tcs.module.profile.entity;

import com.tcs.module.catalog.entity.Location;
import com.tcs.module.identity.entity.User;
import com.tcs.module.profile.enums.Gender;
import com.tcs.module.profile.enums.ProfileVerificationStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tutors")
@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tutor_id")
    private Long tutorId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone", length = 15, nullable = false)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears = 0;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "hourly_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "rating_avg", precision = 3, scale = 2, nullable = false)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20, nullable = false)
    private ProfileVerificationStatus verificationStatus = ProfileVerificationStatus.UNDER_VERIFY;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
