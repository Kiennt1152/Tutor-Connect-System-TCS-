package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.mapper.CatalogMapper;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.TutorSubjectRepository;
import com.tcs.module.catalog.service.CatalogService;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final CategoryRepository categoryRepository;
    private final TutorSubjectRepository tutorSubjectRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final CatalogMapper catalogMapper;

    @Override
    public List<CatalogResponse.CategoryResponse> getCategoryTree(String rootName) {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        if (rootName != null && !rootName.isBlank()) {
            return buildRootBranch(categories, rootName);
        }

        return buildTree(null, indexCategoriesByParent(categories));
    }

    private List<CatalogResponse.CategoryResponse> buildRootBranch(List<Category> categories, String rootName) {
        Map<Long, List<Category>> categoriesByParentId = indexCategoriesByParent(categories);
        Optional<Category> matchingRoot = categories.stream()
                .filter(category -> category.getParent() == null)
                .filter(category -> rootName.trim().equalsIgnoreCase(category.getName()))
                .findFirst();

        if (matchingRoot.isEmpty()) {
            return List.of();
        }

        Category rootCategory = matchingRoot.get();
        List<CatalogResponse.CategoryResponse> children =
                buildTree(rootCategory.getCategoryId(), categoriesByParentId);
        return List.of(toCategoryResponse(rootCategory, children));
    }

    private Map<Long, List<Category>> indexCategoriesByParent(List<Category> categories) {
        Map<Long, List<Category>> categoriesByParentId = new LinkedHashMap<>();

        for (Category category : categories) {
            Long parentId = category.getParent() == null ? null : category.getParent().getCategoryId();
            categoriesByParentId.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(category);
        }

        return categoriesByParentId;
    }

    @Override
    public CatalogResponse.CategoryResponse getCategoryById(Long categoryId) {
        Category category = getRequiredCategory(categoryId);
        return toCategoryResponse(category, List.of());
    }

    @Override
    public CatalogResponse.CategoryResponse createCategory(CatalogRequest.UpsertCategoryRequest request) {
        validateUpsertRequest(request, null);

        Category category = new Category();
        applyCategoryChanges(category, request);
        Category savedCategory = categoryRepository.save(category);
        return toCategoryResponse(savedCategory, List.of());
    }

    @Override
    public CatalogResponse.CategoryResponse updateCategory(Long categoryId, CatalogRequest.UpsertCategoryRequest request) {
        Category category = getRequiredCategory(categoryId);
        validateUpsertRequest(request, category);
        applyCategoryChanges(category, request);
        Category savedCategory = categoryRepository.save(category);
        return toCategoryResponse(savedCategory, List.of());
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = getRequiredCategory(categoryId);
        if (categoryRepository.existsByParent_CategoryId(categoryId)) {
            throw new IllegalArgumentException("Cannot delete category that still has child categories.");
        }

        if (tutorSubjectRepository.existsByCategory_CategoryId(categoryId)) {
            throw new IllegalArgumentException("Cannot delete category that is assigned to tutor subjects.");
        }

        if (tutoringClassRepository.existsByCategory_CategoryId(categoryId)) {
            throw new IllegalArgumentException("Cannot delete category that is assigned to tutoring classes.");
        }

        categoryRepository.delete(category);
    }

    private List<CatalogResponse.CategoryResponse> buildTree(
            Long parentId,
            Map<Long, List<Category>> categoriesByParentId
    ) {
        List<Category> categories = categoriesByParentId.getOrDefault(parentId, List.of());
        List<CatalogResponse.CategoryResponse> responses = new ArrayList<>();

        for (Category category : categories) {
            List<CatalogResponse.CategoryResponse> children =
                    buildTree(category.getCategoryId(), categoriesByParentId);
            responses.add(toCategoryResponse(category, children));
        }

        return responses;
    }

    private CatalogResponse.CategoryResponse toCategoryResponse(
            Category category,
            List<CatalogResponse.CategoryResponse> children
    ) {
        boolean usedByTutorSubjects = tutorSubjectRepository.existsByCategory_CategoryId(category.getCategoryId());
        boolean usedByTutoringClasses = tutoringClassRepository.existsByCategory_CategoryId(category.getCategoryId());
        boolean hasChildren = categoryRepository.existsByParent_CategoryId(category.getCategoryId());
        boolean deletable = !usedByTutorSubjects && !usedByTutoringClasses && !hasChildren;

        return catalogMapper.toCategoryResponse(
                category,
                usedByTutorSubjects,
                usedByTutoringClasses,
                deletable,
                children
        );
    }

    private void applyCategoryChanges(Category category, CatalogRequest.UpsertCategoryRequest request) {
        category.setName(normalizeName(request.getName()));
        category.setDescription(normalizeText(request.getDescription()));
        category.setStatus(normalizeStatus(request.getStatus()));

        if (request.getParentId() == null) {
            category.setParent(null);
            return;
        }

        Category parent = getRequiredCategory(request.getParentId());
        category.setParent(parent);
    }

    private void validateUpsertRequest(CatalogRequest.UpsertCategoryRequest request, Category currentCategory) {
        if (request == null) {
            throw new IllegalArgumentException("Category payload is required.");
        }

        String normalizedName = normalizeName(request.getName());
        if (currentCategory == null) {
            if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("Category name already exists.");
            }
        } else if (categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot(normalizedName, currentCategory.getCategoryId())) {
            throw new IllegalArgumentException("Category name already exists.");
        }

        if (currentCategory != null && request.getParentId() != null) {
            if (currentCategory.getCategoryId().equals(request.getParentId())) {
                throw new IllegalArgumentException("Category cannot be its own parent.");
            }
            ensureNotDescendantParent(currentCategory.getCategoryId(), request.getParentId());
        }

        normalizeStatus(request.getStatus());
    }

    private void ensureNotDescendantParent(Long categoryId, Long parentId) {
        Category cursor = getRequiredCategory(parentId);
        while (cursor != null) {
            if (cursor.getCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("Category parent cannot be one of its descendants.");
            }
            cursor = cursor.getParent();
        }
    }

    private Category getRequiredCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
    }

    private String normalizeName(String value) {
        String normalizedValue = normalizeText(value);
        if (normalizedValue == null) {
            throw new IllegalArgumentException("Category name is required.");
        }
        return normalizedValue;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.equals("ACTIVE") && !normalizedStatus.equals("INACTIVE")) {
            throw new IllegalArgumentException("Category status must be ACTIVE or INACTIVE.");
        }
        return normalizedStatus;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
