package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.LocationResponse;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.entity.Grade;
import com.tcs.module.catalog.entity.Location;
import com.tcs.module.catalog.entity.Province;
import com.tcs.module.catalog.entity.Subject;
import com.tcs.module.catalog.enums.CategoryType;
import com.tcs.module.catalog.mapper.CatalogMapper;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final ProvinceRepository provinceRepository;
    private final LocationRepository locationRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final CatalogMapper catalogMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getSubjects() {
        return subjectRepository.findAll().stream().map(this::toItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> CatalogItemResponse.builder()
                        .id(c.getCategoryId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getGrades() {
        return gradeRepository.findAll().stream()
                .map(g -> CatalogItemResponse.builder()
                        .id(g.getGradeId())
                        .name(g.getGradeName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getProvinces() {
        return provinceRepository.findAll().stream()
                .map(p -> CatalogItemResponse.builder()
                        .id(p.getProvinceId())
                        .name(p.getProvinceName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocations(Long provinceId) {
        List<Location> locations =
                provinceId != null ? locationRepository.findByProvince_ProvinceId(provinceId) : locationRepository.findAll();
        return locations.stream().map(this::toLocation).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqEntries() {
        return faqEntryRepository.findAll().stream().map(this::toFaq).toList();
    }

    private CatalogItemResponse toItem(Subject subject) {
        return CatalogItemResponse.builder()
                .id(subject.getSubjectId())
                .name(subject.getSubjectName())
                .description(subject.getDescription())
                .build();
    }

    private LocationResponse toLocation(Location location) {
        Province province = location.getProvince();
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .provinceId(province != null ? province.getProvinceId() : null)
                .provinceName(province != null ? province.getProvinceName() : null)
                .districtName(location.getDistrictName())
                .wardName(location.getWardName())
                .build();
    }

    private FaqResponse toFaq(FaqEntry entry) {
        return FaqResponse.builder()
                .faqId(entry.getFaqId())
                .question(entry.getQuestion())
                .answer(entry.getAnswer())
                .category(entry.getCategory())
                .build();
    }

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
        boolean usedByTutorSubjects = false;
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
            applyRootOrBranchParent(category, request);
            return;
        }

        Category parent = getRequiredCategory(request.getParentId());
        category.setParent(parent);
        category.setType(parent.getType());
    }

    private void validateUpsertRequest(CatalogRequest.UpsertCategoryRequest request, Category currentCategory) {
        if (request == null) {
            throw new IllegalArgumentException("Category payload is required.");
        }

        String normalizedName = normalizeName(request.getName());
        validateUniqueNameWithinParent(
                normalizedName,
                resolveEffectiveParentIdForValidation(request, currentCategory, normalizedName),
                currentCategory
        );

        if (currentCategory != null && request.getParentId() != null) {
            if (currentCategory.getCategoryId().equals(request.getParentId())) {
                throw new IllegalArgumentException("Category cannot be its own parent.");
            }
            ensureNotDescendantParent(currentCategory.getCategoryId(), request.getParentId());
        }

        normalizeStatus(request.getStatus());
    }

    private void applyRootOrBranchParent(Category category, CatalogRequest.UpsertCategoryRequest request) {
        String normalizedName = normalizeName(request.getName());
        CategoryType targetRootType = resolveRequestedRootType(category, request);

        if (targetRootType == null) {
            category.setParent(null);
            category.setType(resolveRootCategoryType(category, request));
            return;
        }

        if (normalizedName.equalsIgnoreCase(targetRootType.name())) {
            category.setParent(null);
            category.setType(targetRootType);
            return;
        }

        Category rootCategory = findOrCreateRootCategory(targetRootType);
        category.setParent(rootCategory);
        category.setType(rootCategory.getType());
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

    private Category findOrCreateRootCategory(CategoryType rootType) {
        return categoryRepository.findByNameIgnoreCase(rootType.name())
                .filter(category -> category.getParent() == null)
                .orElseGet(() -> {
                    Category root = new Category();
                    root.setName(rootType.name());
                    root.setType(rootType);
                    root.setDescription(defaultRootDescription(rootType));
                    root.setStatus("ACTIVE");
                    root.setActive(true);
                    root.setSortOrder(0);
                    return categoryRepository.save(root);
                });
    }

    private void validateUniqueNameWithinParent(
            String normalizedName,
            Long parentId,
            Category currentCategory
    ) {
        boolean exists;

        if (parentId == null) {
            exists = currentCategory == null
                    ? categoryRepository.existsByParentIsNullAndNameIgnoreCase(normalizedName)
                    : categoryRepository.existsByParentIsNullAndNameIgnoreCaseAndCategoryIdNot(
                            normalizedName,
                            currentCategory.getCategoryId()
                    );
        } else {
            exists = currentCategory == null
                    ? categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(parentId, normalizedName)
                    : categoryRepository.existsByParent_CategoryIdAndNameIgnoreCaseAndCategoryIdNot(
                            parentId,
                            normalizedName,
                            currentCategory.getCategoryId()
                    );
        }

        if (exists) {
            throw new IllegalArgumentException("Category name must be unique within the same parent level.");
        }
    }

    private Long resolveEffectiveParentIdForValidation(
            CatalogRequest.UpsertCategoryRequest request,
            Category currentCategory,
            String normalizedName
    ) {
        if (request.getParentId() != null || currentCategory != null) {
            return request.getParentId();
        }

        CategoryType requestedRootType = resolveRequestedRootType(currentCategory, request);
        if (requestedRootType == null || normalizedName.equalsIgnoreCase(requestedRootType.name())) {
            return null;
        }

        return categoryRepository.findByNameIgnoreCase(requestedRootType.name())
                .filter(category -> category.getParent() == null)
                .map(Category::getCategoryId)
                .orElse(null);
    }

    private CategoryType resolveRootCategoryType(Category category, CatalogRequest.UpsertCategoryRequest request) {
        if (category.getCategoryId() != null) {
            return category.getType();
        }

        try {
            return CategoryType.valueOf(normalizeName(request.getName()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Root category name must match a supported taxonomy group.");
        }
    }

    private CategoryType resolveRequestedRootType(Category category, CatalogRequest.UpsertCategoryRequest request) {
        if (category.getCategoryId() != null) {
            return category.getType();
        }

        String rootName = normalizeText(request.getRootName());
        if (rootName == null) {
            return null;
        }

        try {
            return CategoryType.valueOf(rootName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Root category group is invalid.");
        }
    }

    private String defaultRootDescription(CategoryType rootType) {
        return switch (rootType) {
            case SUBJECT -> "Nhóm gốc cho danh mục môn học.";
            case EDUCATION_LEVEL -> "Nhóm gốc cho danh mục cấp học.";
            case LOCATION -> "Nhóm gốc cho danh mục khu vực.";
            case SYSTEM_CONFIG -> "Nhóm gốc cho danh mục cấu hình hệ thống.";
        };
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
