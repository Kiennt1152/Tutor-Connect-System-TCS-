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
@Table(name = "provinces")
@Getter
@Setter
@NoArgsConstructor
public class Province {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "province_id")
    private Long provinceId;

    @Column(name = "province_name", length = 100, nullable = false, unique = true)
    private String provinceName;
}
