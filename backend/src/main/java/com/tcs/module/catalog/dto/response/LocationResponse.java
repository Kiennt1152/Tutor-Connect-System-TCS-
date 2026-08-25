package com.tcs.module.catalog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponse {

    private Long locationId;
    private Long provinceId;
    private String provinceName;
    private String districtName;
    private String wardName;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getProvinceId() { return provinceId; }
    public void setProvinceId(Long provinceId) { this.provinceId = provinceId; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public String getWardName() { return wardName; }
    public LocationResponse() {}

    public LocationResponse(Long locationId, Long provinceId, String provinceName, String districtName, String wardName) {
        this.locationId = locationId;
        this.provinceId = provinceId;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.wardName = wardName;
    }

    public static LocationResponseBuilder builder() {
        return new LocationResponseBuilder();
    }

    public static class LocationResponseBuilder {
        private Long locationId;
        private Long provinceId;
        private String provinceName;
        private String districtName;
        private String wardName;

        public LocationResponseBuilder locationId(Long locationId) { this.locationId = locationId; return this; }
        public LocationResponseBuilder provinceId(Long provinceId) { this.provinceId = provinceId; return this; }
        public LocationResponseBuilder provinceName(String provinceName) { this.provinceName = provinceName; return this; }
        public LocationResponseBuilder districtName(String districtName) { this.districtName = districtName; return this; }
        public LocationResponseBuilder wardName(String wardName) { this.wardName = wardName; return this; }
        public LocationResponse build() { return new LocationResponse(locationId, provinceId, provinceName, districtName, wardName); }
    }
}
