package com.tcs.module.catalog.mapper;

import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.entity.Category;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public CatalogResponse.CategoryResponse toCategoryResponse(
            Category category,
            boolean usedByTutorSubjects,
            boolean usedByTutoringClasses,
            boolean deletable,
            List<CatalogResponse.CategoryResponse> children
    ) {
        CatalogResponse.CategoryResponse response = new CatalogResponse.CategoryResponse();
        response.setCategoryId(category.getCategoryId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setStatus(category.getStatus());
        response.setParent(toParentResponse(category.getParent()));
        response.setUsedByTutorSubjects(usedByTutorSubjects);
        response.setUsedByTutoringClasses(usedByTutoringClasses);
        response.setDeletable(deletable);
        response.setChildren(children);
        return response;
    }

    public CatalogResponse.ParentCategoryResponse toParentResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CatalogResponse.ParentCategoryResponse(category.getCategoryId(), category.getName());
    }
}
