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
public class CatalogItemResponse {

    private Long id;
    private String name;
    private String description;

    public static CatalogItemResponseBuilder builder() {
        return new CatalogItemResponseBuilder();
    }

    public static class CatalogItemResponseBuilder {
        private Long id;
        private String name;
        private String description;

        public CatalogItemResponseBuilder id(Long id) { this.id = id; return this; }
        public CatalogItemResponseBuilder name(String name) { this.name = name; return this; }
        public CatalogItemResponseBuilder description(String description) { this.description = description; return this; }
        public CatalogItemResponse build() { return new CatalogItemResponse(id, name, description); }
    }
}
