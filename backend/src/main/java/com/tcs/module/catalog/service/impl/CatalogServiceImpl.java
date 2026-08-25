package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.request.UpsertFaqRequest;
import com.tcs.module.catalog.dto.response.CatalogItemResponse;
import com.tcs.module.catalog.dto.response.CatalogResponse;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
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
import com.tcs.module.catalog.repository.DistrictRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.WardRepository;
import com.tcs.module.catalog.service.CatalogService;
import com.tcs.module.catalog.service.GeminiService;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "toi", "cua", "la", "va", "cho", "co", "cac", "nhung", "mot", "nay", "do"
    ));
    private static final Pattern DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final SubjectRepository subjectRepository;
    private final CategoryRepository categoryRepository;
    private final GradeRepository gradeRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final LocationRepository locationRepository;
    private final FaqEntryRepository faqEntryRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final CatalogMapper catalogMapper;
    private final GeminiService geminiService;
    private final AuditLogService auditLogService;

    // LUỒNG 1 - BƯỚC 5.1: Chuẩn hóa tiếng Việt, chuyển chữ thường, thay 'đ' thành 'd' và loại bỏ dấu thanh Unicode NFD
    private String normalizeVietnamese(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        return DIACRITICAL_MARKS.matcher(normalized).replaceAll("");
    }

    // LUỒNG 1 - BƯỚC 5.2: Tách chuỗi từ khóa thành danh sách các token đơn, lọc bỏ số và stop words
    private List<String> tokenize(String text) {
        String normalized = normalizeVietnamese(text);
        String[] parts = normalized.split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank() && !part.chars().allMatch(Character::isDigit) && !STOP_WORDS.contains(part)) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    // LUỒNG 1 - BƯỚC 5.3: Chấm điểm liên quan: Khớp trong Question x 2, khớp trong Answer x 1. Ngưỡng tối thiểu khớp Question >= min(2, tokens.size())
    private int calculateFaqScore(FaqEntry faq, List<String> queryTokens) {
        if (queryTokens.isEmpty()) {
            return 0;
        }
        String qNorm = normalizeVietnamese(faq.getQuestion());
        String aNorm = normalizeVietnamese(faq.getAnswer());

        int questionMatches = 0;
        int answerMatches = 0;
        for (String token : queryTokens) {
            if (qNorm.contains(token)) {
                questionMatches++;
            } else if (aNorm.contains(token)) {
                answerMatches++;
            }
        }
        int requiredQuestionMatches = Math.min(2, queryTokens.size());
        return questionMatches >= requiredQuestionMatches ? questionMatches * 2 + answerMatches : 0;
    }

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
    public List<CatalogItemResponse> getDistricts(Long provinceId) {
        if (provinceId == null) {
            return List.of();
        }
        return districtRepository.findByProvinceIdOrderByDistrictName(provinceId).stream()
                .map(d -> CatalogItemResponse.builder()
                        .id(d.getDistrictId())
                        .name(d.getDistrictName())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getWards(Long districtId) {
        if (districtId == null) {
            return List.of();
        }
        return wardRepository.findByDistrictIdOrderByWardName(districtId).stream()
                .map(w -> CatalogItemResponse.builder()
                        .id(w.getWardId())
                        .name(w.getWardName())
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

    // LUỒNG 1: Tra cứu & Tìm kiếm FAQ theo Danh mục (category) và Từ khóa (keyword)
    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqEntries(String category, String keyword) {
        String trimmedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        // BƯỚC 4: Tầng lọc 1 (Database) - Lấy tập FAQ đã xuất bản theo Category nếu có
        List<FaqEntry> entries;
        if (trimmedCategory != null) {
            entries = faqEntryRepository.findByPublishedTrueAndCategoryOrderBySortOrderAscFaqIdAsc(trimmedCategory);
        } else {
            entries = faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
        }

        // Nếu không nhập từ khóa tìm kiếm -> Chuyển đổi và trả về ngay theo thứ tự sortOrder gốc
        if (!StringUtils.hasText(trimmedKeyword)) {
            return entries.stream().map(this::toFaq).toList();
        }

        // BƯỚC 5: Tách từ khóa và lọc bỏ stop words
        List<String> tokens = tokenize(trimmedKeyword);
        if (tokens.isEmpty()) {
            return entries.stream().map(this::toFaq).toList();
        }

        record ScoredFaq(FaqEntry faq, int score) {}

        // BƯỚC 5: Tầng lọc 2 (In-Memory Ranking Engine) - Chấm điểm liên quan cho từng bài viết
        List<ScoredFaq> scoredList = new ArrayList<>();
        for (FaqEntry entry : entries) {
            int score = calculateFaqScore(entry, tokens);
            if (score > 0) {
                scoredList.add(new ScoredFaq(entry, score));
            }
        }

        // BƯỚC 6: Sắp xếp kết quả đa tầng (score DESC -> sortOrder ASC -> faqId ASC)
        scoredList.sort((a, b) -> {
            // Tiêu chí 1: Điểm liên quan cao nhất lên đầu
            if (b.score() != a.score()) {
                return Integer.compare(b.score(), a.score());
            }
            // Tiêu chí 2: Thứ tự sortOrder do Admin chỉ định
            int sortOrderComp = Integer.compare(
                    a.faq().getSortOrder() != null ? a.faq().getSortOrder() : 0,
                    b.faq().getSortOrder() != null ? b.faq().getSortOrder() : 0
            );
            if (sortOrderComp != 0) {
                return sortOrderComp;
            }
            // Tiêu chí 3: Thứ tự ID bản ghi
            return Long.compare(
                    a.faq().getFaqId() != null ? a.faq().getFaqId() : 0L,
                    b.faq().getFaqId() != null ? b.faq().getFaqId() : 0L
            );
        });

        // BƯỚC 6: Ánh xạ từ Entity FaqEntry sang DTO FaqResponse
        return scoredList.stream().map(sf -> toFaq(sf.faq())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatbotAskResponse askChatbot(ChatbotAskRequest request) {
        String question = request != null ? request.getQuestion() : null;
        if (!StringUtils.hasText(question)) {
            return ChatbotAskResponse.builder()
                    .matched(false)
                    .suggestion("Không tìm thấy câu trả lời. Vui lòng tạo yêu cầu hỗ trợ.")
                    .build();
        }

        List<FaqEntry> entries = faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
        List<String> tokens = tokenize(question);

        if (!tokens.isEmpty()) {
            FaqEntry best = null;
            int maxScore = 0;

            for (FaqEntry entry : entries) {
                int score = calculateFaqScore(entry, tokens);
                if (score > maxScore) {
                    maxScore = score;
                    best = entry;
                }
            }

            if (best != null && maxScore > 0) {
                return ChatbotAskResponse.builder()
                        .matched(true)
                        .aiGenerated(false)
                        .question(best.getQuestion())
                        .answer(best.getAnswer())
                        .faqId(best.getFaqId())
                        .build();
            }
        }

        Optional<String> aiAnswer = geminiService.askQuestion(question.trim());
        if (aiAnswer.isPresent()) {
            return ChatbotAskResponse.builder()
                    .matched(true)
                    .aiGenerated(true)
                    .question(question.trim())
                    .answer(aiAnswer.get())
                    .build();
        }

        return ChatbotAskResponse.builder()
                .matched(false)
                .suggestion("Không tìm thấy câu trả lời. Vui lòng tạo yêu cầu hỗ trợ.")
                .build();
    }

    // =========================================================================
    // LUỒNG 6: QUẢN TRỊ TRI THỨC FAQ - ADMIN CRUD & PHÊ DUYỆT BẢN NHÁP (UC-67)
    // =========================================================================

    // Luồng 6 - Bước 1: Admin lấy toàn bộ danh sách FAQ (bao gồm cả bản nháp chưa xuất bản)
    @Override
    @Transactional(readOnly = true)
    public List<FaqResponse> getFaqEntriesForAdmin(String category, String keyword) {
        String trimmedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return faqEntryRepository.searchAdmin(trimmedCategory, trimmedKeyword).stream().map(this::toFaq).toList();
    }

    // Luồng 6 - Bước 3: Admin tạo bài viết FAQ mới & Ghi vết Audit Log
    @Override
    @Transactional
    public FaqResponse createFaqEntry(UpsertFaqRequest request) {
        FaqEntry entry = new FaqEntry();
        applyFaqChanges(entry, request);
        FaqEntry saved = faqEntryRepository.save(entry);
        auditLogService.record("CREATE_FAQ", "FaqEntry", saved.getFaqId(), null, request);
        return toFaq(saved);
    }

    // Luồng 6 - Bước 4: Admin chỉnh sửa FAQ / Phê duyệt bản nháp do AI sinh ra (published: true)
    @Override
    @Transactional
    public FaqResponse updateFaqEntry(Long faqId, UpsertFaqRequest request) {
        FaqEntry entry = getRequiredFaqEntry(faqId);
        FaqResponse oldValue = toFaq(entry);
        applyFaqChanges(entry, request);
        FaqEntry saved = faqEntryRepository.save(entry);
        auditLogService.record("UPDATE_FAQ", "FaqEntry", saved.getFaqId(), oldValue, request);
        return toFaq(saved);
    }

    // Luồng 6 - Bước 5: Admin xóa bài viết FAQ & Ghi vết Audit Log
    @Override
    @Transactional
    public void deleteFaqEntry(Long faqId) {
        FaqEntry entry = getRequiredFaqEntry(faqId);
        FaqResponse oldValue = toFaq(entry);
        faqEntryRepository.delete(entry);
        auditLogService.record("DELETE_FAQ", "FaqEntry", faqId, oldValue, null);
    }

    private FaqEntry getRequiredFaqEntry(Long faqId) {
        return faqEntryRepository.findById(faqId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi thường gặp: " + faqId));
    }

    private void applyFaqChanges(FaqEntry entry, UpsertFaqRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu FAQ.");
        }

        String question = normalizeText(request.getQuestion());
        if (question == null) {
            throw new IllegalArgumentException("Câu hỏi là bắt buộc.");
        }

        String answer = normalizeText(request.getAnswer());
        if (answer == null) {
            throw new IllegalArgumentException("Câu trả lời là bắt buộc.");
        }

        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setCategory(normalizeText(request.getCategory()));
        entry.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        entry.setPublished(request.getPublished() == null || request.getPublished());
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

    // LUỒNG 1 - BƯỚC 6: Ánh xạ Entity FaqEntry sang DTO FaqResponse trả về cho Client
    private FaqResponse toFaq(FaqEntry entry) {
        return FaqResponse.builder()
                .faqId(entry.getFaqId())
                .question(entry.getQuestion())
                .answer(entry.getAnswer())
                .category(entry.getCategory())
                .sortOrder(entry.getSortOrder())
                .published(entry.getPublished())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // LUỒNG 9: QUẢN LÝ CÂY DANH MỤC HỆ THỐNG PHÂN CẤP ĐỆ QUY (UC-57)
    // =========================================================================

    // Luồng 9 - Bước 1: Dựng cấu trúc cây danh mục đệ quy (Recursive Tree Hierarchy)
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

    // Luồng 9 - Bước 2: Gom nhóm danh mục theo parentId bằng LinkedHashMap (Độ phức tạp O(N))
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

    // Luồng 9 - Bước 3: Tạo danh mục mới kèm kiểm tra tính duy nhất trong cùng danh mục cha
    @Override
    @Transactional
    public CatalogResponse.CategoryResponse createCategory(CatalogRequest.UpsertCategoryRequest request) {
        validateUpsertRequest(request, null);

        Category category = new Category();
        applyCategoryChanges(category, request);
        Category savedCategory = categoryRepository.save(category);
        auditLogService.record("CREATE_CATEGORY", "Category", savedCategory.getCategoryId(), null, request);
        return toCategoryResponse(savedCategory, List.of());
    }

    // Luồng 9 - Bước 4: Cập nhật danh mục kèm chống tạo chu trình đệ quy vô hạn (Anti-circular dependency)
    @Override
    @Transactional
    public CatalogResponse.CategoryResponse updateCategory(Long categoryId, CatalogRequest.UpsertCategoryRequest request) {
        Category category = getRequiredCategory(categoryId);
        CatalogResponse.CategoryResponse oldValue = toCategoryResponse(category, List.of());
        validateUpsertRequest(request, category);
        applyCategoryChanges(category, request);
        Category savedCategory = categoryRepository.save(category);
        auditLogService.record("UPDATE_CATEGORY", "Category", savedCategory.getCategoryId(), oldValue, request);
        return toCategoryResponse(savedCategory, List.of());
    }

    // Luồng 9 - Bước 5: Xóa an toàn danh mục với 2 lớp bảo vệ (Chống node mồ côi & Bảo toàn dữ liệu lớp học)
    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = getRequiredCategory(categoryId);
        // Lớp bảo vệ 1: Không cho xóa nếu vẫn còn danh mục con trực thuộc
        if (categoryRepository.existsByParent_CategoryId(categoryId)) {
            throw new IllegalArgumentException("Không thể xóa danh mục vẫn còn danh mục con.");
        }

        // Lớp bảo vệ 2: Không cho xóa nếu danh mục đang được gắn vào lớp học
        if (tutoringClassRepository.existsByCategory_CategoryId(categoryId)) {
            throw new IllegalArgumentException("Không thể xóa danh mục đang được dùng cho lớp học.");
        }

        CatalogResponse.CategoryResponse oldValue = toCategoryResponse(category, List.of());
        categoryRepository.delete(category);
        auditLogService.record("DELETE_CATEGORY", "Category", categoryId, oldValue, null);
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
            throw new IllegalArgumentException("Thiếu dữ liệu danh mục.");
        }

        String normalizedName = normalizeName(request.getName());
        validateUniqueNameWithinParent(
                normalizedName,
                resolveEffectiveParentIdForValidation(request, currentCategory, normalizedName),
                currentCategory
        );

        if (currentCategory != null && request.getParentId() != null) {
            if (currentCategory.getCategoryId().equals(request.getParentId())) {
                throw new IllegalArgumentException("Danh mục không thể tự chọn chính nó làm danh mục cha.");
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
                throw new IllegalArgumentException("Danh mục cha không thể là danh mục con của chính nó.");
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
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
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
        if (category != null && category.getCategoryId() != null) {
            return category.getType();
        }

        try {
            return CategoryType.valueOf(normalizeName(request.getName()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tên nhóm gốc không hợp lệ.");
        }
    }

    private CategoryType resolveRequestedRootType(Category category, CatalogRequest.UpsertCategoryRequest request) {
        if (category != null && category.getCategoryId() != null) {
            return category.getType();
        }

        String rootName = normalizeText(request.getRootName());
        if (rootName == null) {
            return null;
        }

        try {
            return CategoryType.valueOf(rootName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nhóm gốc không hợp lệ.");
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
            throw new IllegalArgumentException("Tên danh mục là bắt buộc.");
        }
        return normalizedValue;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.equals("ACTIVE") && !normalizedStatus.equals("INACTIVE")) {
            throw new IllegalArgumentException("Trạng thái danh mục không hợp lệ.");
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
