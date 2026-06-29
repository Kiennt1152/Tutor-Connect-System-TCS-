package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import com.tcs.module.catalog.service.CatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/categories")
    public List<CatalogItemResponse> getCategories() {
        return catalogService.getCategories();
    }

    @GetMapping("/grades")
    public List<CatalogItemResponse> getGrades() {
        return catalogService.getGrades();
    }

    @GetMapping("/provinces")
    public List<CatalogItemResponse> getProvinces() {
        return catalogService.getProvinces();
    }

    @GetMapping("/locations")
    public List<LocationResponse> getLocations(@RequestParam(required = false) Long provinceId) {
        return catalogService.getLocations(provinceId);
    }

    @GetMapping("/faq")
    public List<FaqResponse> getFaq() {
        return catalogService.getFaqEntries();
    }
}
