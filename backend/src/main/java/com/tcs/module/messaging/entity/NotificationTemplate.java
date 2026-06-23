package com.tcs.module.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "title_template", length = 200, nullable = false)
    private String titleTemplate;

    @Column(name = "content_template", columnDefinition = "TEXT", nullable = false)
    private String contentTemplate;

    @Column(name = "channel", length = 20, nullable = false)
    private String channel;
}
