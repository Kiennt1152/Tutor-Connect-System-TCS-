package com.tcs.module.center.entity;

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
@Table(name = "lead_routing_rules")
@Getter
@Setter
@NoArgsConstructor
public class LeadRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "criteria", columnDefinition = "TEXT")
    private String criteria;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
