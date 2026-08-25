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
}
