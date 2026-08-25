package com.tcs.module.catalog.entity;

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
@Table(name = "system_parameters")
@Getter
@Setter
@NoArgsConstructor
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parameter_id")
    private Long parameterId;

    @Column(name = "param_key", length = 100, nullable = false, unique = true)
    private String paramKey;

    @Column(name = "param_value", columnDefinition = "TEXT", nullable = false)
    private String paramValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
