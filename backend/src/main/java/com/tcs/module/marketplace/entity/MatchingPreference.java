package com.tcs.module.marketplace.entity;

import com.tcs.module.identity.entity.User;
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
@Table(name = "matching_preferences")
@Getter
@Setter
@NoArgsConstructor
public class MatchingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_gender", length = 10)
    private String preferredGender;

    @Column(name = "min_rating", precision = 3, scale = 2)
    private java.math.BigDecimal minRating;

    @Column(name = "max_hourly_rate", precision = 12, scale = 2)
    private java.math.BigDecimal maxHourlyRate;

    @Column(name = "preferred_location", length = 255)
    private String preferredLocation;
}
