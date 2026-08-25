package com.tcs.module.catalog.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CatalogResponse {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentCategoryResponse {
        private Long categoryId;
        private String name;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long categoryId;
        private String name;
        private String description;
        private String status;
        private ParentCategoryResponse parent;
        private boolean usedByTutorSubjects;
        private boolean usedByTutoringClasses;
        private boolean deletable;
        private List<CategoryResponse> children = new ArrayList<>();

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public ParentCategoryResponse getParent() { return parent; }
        public void setParent(ParentCategoryResponse parent) { this.parent = parent; }
        public boolean isUsedByTutorSubjects() { return usedByTutorSubjects; }
        public void setUsedByTutorSubjects(boolean usedByTutorSubjects) { this.usedByTutorSubjects = usedByTutorSubjects; }
        public boolean isUsedByTutoringClasses() { return usedByTutoringClasses; }
        public void setUsedByTutoringClasses(boolean usedByTutoringClasses) { this.usedByTutoringClasses = usedByTutoringClasses; }
        public boolean isDeletable() { return deletable; }
        public void setDeletable(boolean deletable) { this.deletable = deletable; }
        public List<CategoryResponse> getChildren() { return children; }
        public void setChildren(List<CategoryResponse> children) { this.children = children; }
    }
}
