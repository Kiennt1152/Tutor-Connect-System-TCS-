package com.tcs.module.catalog.dto.response;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CatalogResponse {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentCategoryResponse {

        private Long categoryId;

        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
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
    }
}
