package com.tcs.module.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiChatSession session;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "referenced_tutor_ids")
    private String referencedTutorIds;

    @Column(name = "referenced_class_ids")
    private String referencedClassIds;

    @Column(name = "referenced_faq_ids")
    private String referencedFaqIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public AiChatSession getSession() { return session; }
    public void setSession(AiChatSession session) { this.session = session; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReferencedTutorIds() { return referencedTutorIds; }
    public void setReferencedTutorIds(String referencedTutorIds) { this.referencedTutorIds = referencedTutorIds; }
    public String getReferencedClassIds() { return referencedClassIds; }
    public void setReferencedClassIds(String referencedClassIds) { this.referencedClassIds = referencedClassIds; }
    public String getReferencedFaqIds() { return referencedFaqIds; }
    public void setReferencedFaqIds(String referencedFaqIds) { this.referencedFaqIds = referencedFaqIds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
