package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.service.CatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/categories")
    public List<CatalogResponse.CategoryResponse> getCategoryTree(
            @RequestParam(required = false) String root
    ) {
        return catalogService.getCategoryTree(root);
    }

    @GetMapping("/categories/{categoryId}")
    public CatalogResponse.CategoryResponse getCategoryById(@PathVariable Long categoryId) {
        return catalogService.getCategoryById(categoryId);
    }

    @PostMapping("/categories")
    public CatalogResponse.CategoryResponse createCategory(
            @RequestBody CatalogRequest.UpsertCategoryRequest request
    ) {
        return catalogService.createCategory(request);
    }

    @PutMapping("/categories/{categoryId}")
    public CatalogResponse.CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CatalogRequest.UpsertCategoryRequest request
    ) {
        return catalogService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId) {
        catalogService.deleteCategory(categoryId);
    }
}
