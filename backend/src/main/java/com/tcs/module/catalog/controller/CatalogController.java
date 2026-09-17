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
    // LUỒNG 1: TRA CỨU & TÌM KIẾM FAQ TRI THỨC (/help - UC-65, UC-67)
    // =========================================================================
    /**
     * [UC-67] Tra cứu danh sách FAQ công khai dành cho học viên và gia sư (/help).
     * Hỗ trợ tìm kiếm theo danh mục phân loại hoặc từ khóa câu hỏi/nội dung.
     * 
     * @param category Danh mục FAQ (tùy chọn, ví dụ: 'PAYMENT', 'POLICY', 'ACCOUNT')
     * @param keyword Từ khóa tìm kiếm trong câu hỏi hoặc câu trả lời
     * @return Danh sách FAQ {@link FaqResponse} thỏa mãn điều kiện lọc
     */
    @GetMapping("/faq")
    public List<FaqResponse> getFaqEntries(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return catalogService.getFaqEntries(category, keyword);
    }

    /**
     * [UC-65] Hỏi đáp nhanh với trợ lý Chatbot hỗ trợ người dùng trên nền tảng.
     * 
     * @param request Câu hỏi người dùng {@link ChatbotAskRequest}
     * @return Câu trả lời gợi ý {@link ChatbotAskResponse}
     */
    @PostMapping("/chatbot/ask")
    public ChatbotAskResponse askChatbot(@Valid @RequestBody ChatbotAskRequest request) {
        return catalogService.askChatbot(request);
    }

    // =========================================================================
    // LUỒNG 6: QUẢN TRỊ TRI THỨC FAQ - ADMIN CRUD (/platform/faq - UC-67)
    // =========================================================================
    /**
     * [UC-67] Danh sách toàn bộ FAQ cho trang quản trị Admin (/platform/faq).
     * Hiển thị cả các mục đang ẩn/vô hiệu hóa, hỗ trợ lọc theo danh mục và từ khóa.
     * 
     * @param category Danh mục phân loại FAQ
     * @param keyword Từ khóa tìm kiếm
     * @return Danh sách đầy đủ các mục FAQ {@link FaqResponse}
     */
    @GetMapping("/faq/admin")
    public List<FaqResponse> getFaqEntriesForAdmin(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword
    ) {
        return catalogService.getFaqEntriesForAdmin(category, keyword);
    }

    /**
     * [UC-67] Thêm mới câu hỏi thường gặp FAQ vào kho tri thức nền tảng.
     * Tự động tạo vector embedding để phục vụ tìm kiếm ngữ nghĩa RAG nếu bật AI pipeline.
     * 
     * @param request Dữ liệu FAQ mới {@link UpsertFaqRequest}
     * @return FAQ vừa tạo thành công {@link FaqResponse}
     */
    @PostMapping("/faq")
    public FaqResponse createFaqEntry(@Valid @RequestBody UpsertFaqRequest request) {
        return catalogService.createFaqEntry(request);
    }

    /**
     * [UC-67] Cập nhật nội dung, tiêu đề hoặc trạng thái hiển thị của một câu hỏi FAQ.
     * 
     * @param faqId ID của mục FAQ cần chỉnh sửa
     * @param request Dữ liệu cập nhật {@link UpsertFaqRequest}
     * @return FAQ sau khi cập nhật thành công {@link FaqResponse}
     */
    @PatchMapping("/faq/{faqId}")
    public FaqResponse updateFaqEntry(@PathVariable Long faqId, @Valid @RequestBody UpsertFaqRequest request) {
        return catalogService.updateFaqEntry(faqId, request);
    }

    /**
     * [UC-67] Xóa một câu hỏi FAQ khỏi kho tri thức.
     * Đồng thời loại bỏ các chunk tương ứng trong cơ sở dữ liệu vector/retrieval.
     * 
     * @param faqId ID của mục FAQ cần xóa
     */
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

    @PutMapping("/categories/{categoryId}")
    public CatalogResponse.CategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @RequestBody CatalogRequest.UpsertCategoryRequest request
    ) {
        return catalogService.updateCategory(categoryId, request);
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
