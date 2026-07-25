package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
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

    @GetMapping("/subjects")
    public List<CatalogItemResponse> getSubjects() {
        return catalogService.getSubjects();
    }

    @GetMapping("/grades")
    public List<CatalogItemResponse> getGrades() {
        return catalogService.getGrades();
    }

    @GetMapping("/provinces")
    public List<CatalogItemResponse> getProvinces() {
        return catalogService.getProvinces();
    }

    @GetMapping("/faq")
    public List<FaqResponse> getFaq() {
        return catalogService.getFaqEntries();
    }

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

    @GetMapping("/districts")
    public List<CatalogItemResponse> getDistricts(@RequestParam Long provinceId) {
        return catalogService.getDistricts(provinceId);
    }

    @GetMapping("/wards")
    public List<CatalogItemResponse> getWards(@RequestParam Long districtId) {
        return catalogService.getWards(districtId);
    }

    @GetMapping("/locations")
    public List<LocationResponse> getLocations(@RequestParam(required = false) Long provinceId) {
        return catalogService.getLocations(provinceId);
    }

    @DeleteMapping("/categories/{categoryId}")
    public void deleteCategory(@PathVariable Long categoryId) {
        catalogService.deleteCategory(categoryId);
    }
}
