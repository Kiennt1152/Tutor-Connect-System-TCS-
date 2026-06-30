package com.tcs.module.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationResponse {

    private Long locationId;
    private Long provinceId;
    private String provinceName;
    private String districtName;
    private String wardName;
}
