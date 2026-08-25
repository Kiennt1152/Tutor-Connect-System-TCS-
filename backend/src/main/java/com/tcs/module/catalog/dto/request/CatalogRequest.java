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

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRootName() { return rootName; }
        public void setRootName(String rootName) { this.rootName = rootName; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
