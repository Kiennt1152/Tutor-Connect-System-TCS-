package com.tcs.module.catalog.service;

import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import java.util.List;

public interface CatalogService {

    List<CatalogItemResponse> getSubjects();

    List<CatalogItemResponse> getCategories();

    List<CatalogItemResponse> getGrades();

    List<CatalogItemResponse> getProvinces();

    List<LocationResponse> getLocations(Long provinceId);

    /** Tra cứu FAQ đã xuất bản, có thể lọc theo category và/hoặc tìm theo từ khóa trong câu hỏi/câu trả lời. */
    List<FaqResponse> getFaqEntries(String category, String keyword);

    /** Chatbot rule-based: so khớp câu hỏi với FAQ theo số từ khóa trùng, trả FAQ khớp nhất hoặc gợi ý tạo ticket. */
    ChatbotAskResponse askChatbot(ChatbotAskRequest request);

    List<CatalogResponse.CategoryResponse> getCategoryTree(String rootName);

    CatalogResponse.CategoryResponse getCategoryById(Long categoryId);

    CatalogResponse.CategoryResponse createCategory(CatalogRequest.UpsertCategoryRequest request);

    CatalogResponse.CategoryResponse updateCategory(Long categoryId, CatalogRequest.UpsertCategoryRequest request);

    void deleteCategory(Long categoryId);
}
