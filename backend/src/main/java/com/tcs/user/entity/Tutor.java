package com.tcs.user.entity;

import java.math.BigDecimal;

import com.tcs.masterdata.entity.Location;
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
@Table(name = "tutors")
@Getter
@Setter
@NoArgsConstructor
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tutor_id", length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "gender", length = 10, nullable = false)
    private String gender;

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

    @Column(name = "infraction_count", nullable = false)
    private Integer infractionCount = 0;

    @Column(name = "reputation_score", precision = 3, scale = 2, nullable = false)
    private BigDecimal reputationScore = new BigDecimal("5.00");
}
