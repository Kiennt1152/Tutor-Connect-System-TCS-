package com.tcs.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tcs.user.entity.Tutor;
import com.tcs.user.entity.User;
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
@Table(name = "recommendation_logs")
@Getter
@Setter
@NoArgsConstructor
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "recommendation_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Tutor tutor;

    @Column(name = "score", precision = 5, scale = 2, nullable = false)
    private BigDecimal score = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
