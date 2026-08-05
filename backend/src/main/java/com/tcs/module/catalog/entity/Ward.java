package com.tcs.module.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
public class Ward {

    @Id
    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "ward_name", length = 120, nullable = false)
    private String wardName;

    @Column(name = "district_id", nullable = false)
    private Long districtId;
}
