package com.tcs.module.catalog.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CatalogRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class UpsertCategoryRequest {

        private String name;

        private String description;

        private String rootName;

        private Long parentId;

        private String status;
    }
}
