package com.tcs.module.catalog.controller;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.request.UpsertFaqRequest;
import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import com.tcs.module.catalog.service.CatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    // =========================================================================
    // LUỒNG 1: TRA CỨU & TÌM KIẾM FAQ TRI THỨC (/help - UC-61, UC-67)
    // =========================================================================
    @GetMapping("/faq")
    public List<FaqResponse> getFaqEntries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return catalogService.getFaqEntries(category, keyword);
    }

    @PostMapping("/chatbot/ask")
    public ChatbotAskResponse askChatbot(@Valid @RequestBody ChatbotAskRequest request) {
        return catalogService.askChatbot(request);
    }

    // =========================================================================
    // LUỒNG 6: QUẢN TRỊ TRI THỨC FAQ - ADMIN CRUD (/platform/faq - UC-67)
    // =========================================================================
    @GetMapping("/faq/admin")
    public List<FaqResponse> getFaqEntriesForAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return catalogService.getFaqEntriesForAdmin(category, keyword);
    }

    @PostMapping("/faq")
    public FaqResponse createFaqEntry(@Valid @RequestBody UpsertFaqRequest request) {
        return catalogService.createFaqEntry(request);
    }

    @PatchMapping("/faq/{faqId}")
    public FaqResponse updateFaqEntry(@PathVariable Long faqId, @Valid @RequestBody UpsertFaqRequest request) {
        return catalogService.updateFaqEntry(faqId, request);
    }

    @DeleteMapping("/faq/{faqId}")
    public void deleteFaqEntry(@PathVariable Long faqId) {
        catalogService.deleteFaqEntry(faqId);
    }

    // =========================================================================
    // LUỒNG 9: QUẢN LÝ CÂY DANH MỤC HỆ THỐNG PHÂN CẤP (/categories - UC-57)
    // =========================================================================
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
