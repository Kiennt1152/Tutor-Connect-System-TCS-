package com.tcs.masterdata.entity;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "province_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "province_name", length = 100, nullable = false, unique = true)
    private String provinceName;
}
