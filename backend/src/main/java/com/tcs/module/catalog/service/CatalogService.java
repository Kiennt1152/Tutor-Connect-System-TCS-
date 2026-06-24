package com.tcs.module.catalog.service;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import java.util.List;

public interface CatalogService {

    List<CatalogResponse.CategoryResponse> getCategoryTree(String rootName);

    CatalogResponse.CategoryResponse getCategoryById(Long categoryId);

    CatalogResponse.CategoryResponse createCategory(CatalogRequest.UpsertCategoryRequest request);

    CatalogResponse.CategoryResponse updateCategory(Long categoryId, CatalogRequest.UpsertCategoryRequest request);

    void deleteCategory(Long categoryId);
}
