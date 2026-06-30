package com.tcs.module.catalog.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CatalogItemResponse {

    private Long id;
    private String name;
    private String description;
}
