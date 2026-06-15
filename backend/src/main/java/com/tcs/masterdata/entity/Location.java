package com.tcs.masterdata.entity;

import java.math.BigDecimal;

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

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "google_place_id", length = 255, unique = true)
    private String googlePlaceId;

    @Column(name = "address_line", length = 255, nullable = false)
    private String addressLine;

    @Column(name = "ward_name", length = 100)
    private String wardName;

    @Column(name = "district_name", length = 100)
    private String districtName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "province_id", nullable = false)
    private Province province;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;
}
