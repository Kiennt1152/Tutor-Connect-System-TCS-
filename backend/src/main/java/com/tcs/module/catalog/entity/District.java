package com.tcs.module.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quận / Huyện (id = mã hành chính GSO, gán về tỉnh mới sau sáp nhập 2025). */
@Entity
@Table(name = "districts")
@Getter
@Setter
@NoArgsConstructor
public class District {

    @Id
    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "district_name", length = 120, nullable = false)
    private String districtName;

    @Column(name = "province_id", nullable = false)
    private Long provinceId;
}
