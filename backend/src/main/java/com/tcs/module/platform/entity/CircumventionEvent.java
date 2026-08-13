package com.tcs.module.platform.entity;

import com.tcs.module.identity.entity.User;
import com.tcs.module.messaging.entity.Conversation;
import com.tcs.module.messaging.entity.Message;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "circumvention_events")
@Getter @Setter @NoArgsConstructor
public class CircumventionEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id") private Long eventId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "message_id") private Message message;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id") private Conversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id") private User sender;
    @Column(name = "matched_rule", nullable = false, length = 30) private String matchedRule;
    @Column(name = "evidence", nullable = false, length = 500) private String evidence;
    @Column(name = "risk_score", nullable = false) private Integer riskScore;
    @Column(name = "status", nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "review_note", length = 500) private String reviewNote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private User reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
}
