package com.tcs.communication.entity;

import java.time.LocalDateTime;

import com.tcs.user.entity.PlatformAdmin;
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
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ticket_id", length = 36, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "subject", length = 150, nullable = false)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "priority", length = 20, nullable = false)
    private String priority = "MEDIUM";

    @Column(name = "status", length = 20, nullable = false)
    private String status = "OPEN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    private PlatformAdmin assignedAdmin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
