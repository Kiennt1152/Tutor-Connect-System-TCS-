package com.tcs.module.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.request.UpsertFaqRequest;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.FaqEntry;
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
import com.tcs.module.catalog.service.GeminiService;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@Tag("report52-support")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Report52CatalogConfigITTest {

    @Mock private SubjectRepository subjectRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private WardRepository wardRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private FaqEntryRepository faqEntryRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private GeminiService geminiService;
    @Mock private AuditLogService auditLogService;

    private final CatalogMapper catalogMapper = new CatalogMapper();

    private CatalogServiceImpl catalogService;

    @BeforeEach
    void setUpCatalogConfigItFixture() {
        catalogService = new CatalogServiceImpl(
                subjectRepository,
                categoryRepository,
                gradeRepository,
                provinceRepository,
                districtRepository,
                wardRepository,
                locationRepository,
                faqEntryRepository,
                tutoringClassRepository,
                catalogMapper,
                geminiService,
                auditLogService);
    }

    @Test
    void SUPPORT_CAT_LoadCategoryTreeWithParentChildHierarchyForCatalogFormsAtServiceLevel() {
        Category root = category(1L, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");
        Category math = category(2L, "Toán", CategoryType.SUBJECT, root, "ACTIVE");
        Category english = category(3L, "Tiếng Anh", CategoryType.SUBJECT, root, "INACTIVE");

        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(root, math, english));
        when(categoryRepository.existsByParent_CategoryId(1L)).thenReturn(true);

        var tree = catalogService.getCategoryTree(null);

        assertEquals(1, tree.size());
        assertEquals("SUBJECT", tree.get(0).getName());
        assertEquals(2, tree.get(0).getChildren().size());
        assertEquals("Toán", tree.get(0).getChildren().get(0).getName());
        assertEquals("Tiếng Anh", tree.get(0).getChildren().get(1).getName());
    }

    @Test
    void SUPPORT_CAT_FilterPublishedFaqByCategoryAndVietnameseKeywordAtServiceLevel() {
        FaqEntry payment = faq(10L, "Thanh toán học phí như thế nào?", "Phụ huynh quét mã QR để thanh toán.", "PAYMENT", 1, true);
        FaqEntry profile = faq(11L, "Cập nhật hồ sơ gia sư", "Vào trang xác minh để cập nhật.", "PROFILE", 2, true);

        when(faqEntryRepository.findByPublishedTrueAndCategoryOrderBySortOrderAscFaqIdAsc("PAYMENT"))
                .thenReturn(List.of(payment, profile));

        List<FaqResponse> responses = catalogService.getFaqEntries("PAYMENT", "thanh toan");

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getFaqId());
        assertEquals("Thanh toán học phí như thế nào?", responses.get(0).getQuestion());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_003_ReadCategoryDetailWithParentUsageAndDeletableFlags() {
        Category root = category(1L, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");
        Category math = category(2L, "Toán", CategoryType.SUBJECT, root, "ACTIVE");

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(math));
        when(tutoringClassRepository.existsByCategory_CategoryId(2L)).thenReturn(true);

        var response = catalogService.getCategoryById(2L);

        assertEquals(2L, response.getCategoryId());
        assertEquals("Toán", response.getName());
        assertEquals(1L, response.getParent().getCategoryId());
        assertTrue(response.isUsedByTutoringClasses());
        assertFalse(response.isDeletable());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_004_RejectCreateCategoryWhenNameIsMissing() {
        CatalogRequest.UpsertCategoryRequest request = categoryRequest(" ", "SUBJECT", null, "ACTIVE");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createCategory(request));

        assertEquals("Tên danh mục là bắt buộc.", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_006_AnonymousCanReadPublicCatalogButAdminCatalogRoutesRemainProtected() throws IOException {
        String securityConfig = readSecurityConfigSource();

        int protectedFaqAdmin = securityConfig.indexOf("\"/api/catalog/faq/admin\"");
        int protectedParameters = securityConfig.indexOf("\"/api/catalog/parameters/**\"");
        int publicCatalogGet = securityConfig.indexOf("\"/api/catalog/**\"");

        assertTrue(protectedFaqAdmin >= 0);
        assertTrue(protectedParameters >= 0);
        assertTrue(publicCatalogGet > protectedFaqAdmin);
        assertTrue(securityConfig.substring(protectedFaqAdmin, publicCatalogGet)
                .contains(".hasRole(RbacConstants.PLATFORM_ADMIN)"));
        assertTrue(securityConfig.substring(protectedParameters, publicCatalogGet)
                .contains(".hasRole(RbacConstants.PLATFORM_ADMIN)"));
        assertTrue(securityConfig.substring(publicCatalogGet, securityConfig.indexOf("\"/api/ai/**\""))
                .contains(".permitAll()"));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_007_NonAdminCanReadPublicCatalogButCannotUseCatalogWriteEndpoints() throws IOException {
        String securityConfig = readSecurityConfigSource();

        int publicCatalogRead = securityConfig.indexOf(".requestMatchers(HttpMethod.GET, \"/api/catalog/**\")");
        int publicChatbotPost = securityConfig.indexOf(".requestMatchers(HttpMethod.POST, \"/api/catalog/chatbot/ask\")");
        int adminCatalogWrite = securityConfig.indexOf(".requestMatchers(\"/api/catalog/**\")");

        assertTrue(publicCatalogRead >= 0);
        assertTrue(publicChatbotPost > publicCatalogRead);
        assertTrue(adminCatalogWrite > publicChatbotPost);
        assertTrue(securityConfig.substring(publicCatalogRead, publicChatbotPost).contains(".permitAll()"));
        assertTrue(securityConfig.substring(publicChatbotPost, adminCatalogWrite).contains(".permitAll()"));
        assertTrue(securityConfig.substring(adminCatalogWrite, securityConfig.indexOf("\"/api/marketplace/classes/**\""))
                .contains(".hasRole(RbacConstants.PLATFORM_ADMIN)"));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_008_RejectDuplicateCategoryNameWithinSameParent() {
        CatalogRequest.UpsertCategoryRequest request = categoryRequest("Toán", null, 1L, "ACTIVE");

        when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(1L, "Toán")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createCategory(request));

        assertEquals("Tên danh mục đã tồn tại.", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_010_UpdateCategoryRecordsAuditWithOldAndNewValues() {
        Category root = category(1L, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");
        Category category = category(2L, "Toán", CategoryType.SUBJECT, root, "ACTIVE");
        CatalogRequest.UpsertCategoryRequest request = categoryRequest("Toán nâng cao", null, 1L, "INACTIVE");

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCaseAndCategoryIdNot(1L, "Toán nâng cao", 2L))
                .thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        var response = catalogService.updateCategory(2L, request);

        assertEquals("Toán nâng cao", response.getName());
        assertEquals("INACTIVE", response.getStatus());
        verify(auditLogService).record(eq("UPDATE_CATEGORY"), eq("Category"), eq(2L), any(), eq(request));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_016_RejectMovingCategoryUnderItsOwnDescendant() {
        Category parent = category(2L, "Toán", CategoryType.SUBJECT, null, "ACTIVE");
        Category child = category(3L, "Toán lớp 12", CategoryType.SUBJECT, parent, "ACTIVE");
        CatalogRequest.UpsertCategoryRequest request = categoryRequest("Toán", null, 3L, "ACTIVE");

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(child));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.updateCategory(2L, request));

        assertEquals("Danh mục cha không thể là danh mục con của chính nó.", exception.getMessage());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_012_RejectDeletingCategoryThatStillHasChildren() {
        Category root = category(1L, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root));
        when(categoryRepository.existsByParent_CategoryId(1L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.deleteCategory(1L));

        assertEquals("Không thể xóa danh mục vẫn còn danh mục con.", exception.getMessage());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_013_ChatbotUsesPublishedFaqBeforeCallingAiProvider() {
        FaqEntry faq = faq(
                20L,
                "Làm sao để rút tiền?",
                "Vào ví và tạo yêu cầu rút tiền.",
                "FINANCE",
                1,
                true);
        ChatbotAskRequest request = new ChatbotAskRequest();
        request.setQuestion("toi muon rut tien");

        when(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc()).thenReturn(List.of(faq));

        ChatbotAskResponse response = catalogService.askChatbot(request);

        assertTrue(response.isMatched());
        assertFalse(response.isAiGenerated());
        assertEquals(20L, response.getFaqId());
        verify(geminiService, never()).askQuestion(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_015_CreateFaqEntryWritesAuditLog() {
        UpsertFaqRequest request = faqRequest("Cách thanh toán?", "Quét mã QR.", "PAYMENT", true);

        when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(invocation -> {
            FaqEntry entry = invocation.getArgument(0);
            entry.setFaqId(30L);
            return entry;
        });

        FaqResponse response = catalogService.createFaqEntry(request);

        assertEquals(30L, response.getFaqId());
        assertEquals("Cách thanh toán?", response.getQuestion());
        verify(auditLogService).record(eq("CREATE_FAQ"), eq("FaqEntry"), eq(30L), eq(null), eq(request));
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_019_CatalogValidationMessagesStayVietnameseAndDoNotExposeRawEnumNames() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createFaqEntry(faqRequest(" ", "Câu trả lời cho người dùng.", "PAYMENT", true)));

        assertEquals("Câu hỏi là bắt buộc.", exception.getMessage());
        assertFalse(exception.getMessage().contains("PAYMENT"));
        assertFalse(exception.getMessage().contains("FaqEntry"));
        verify(faqEntryRepository, never()).save(any());
    }

    @Test
    void SUPPORT_CAT_RejectDeletingCategoryUsedByTutoringClass() {
        Category math = category(2L, "Toán", CategoryType.SUBJECT, null, "ACTIVE");

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(math));
        when(tutoringClassRepository.existsByCategory_CategoryId(2L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.deleteCategory(2L));

        assertEquals("Không thể xóa danh mục đang được dùng cho lớp học.", exception.getMessage());
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @Tag("report52-it")
    void IT_CAT_020_CreateCategoryUnderRequestedRootCreatesMissingRootAndChild() {
        CatalogRequest.UpsertCategoryRequest request = categoryRequest("Toán luyện thi", "SUBJECT", null, "ACTIVE");
        Category savedRoot = category(1L, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");

        when(categoryRepository.existsByParentIsNullAndNameIgnoreCase("Toán luyện thi")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCase("SUBJECT")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            if ("SUBJECT".equals(category.getName())) {
                return savedRoot;
            }
            category.setCategoryId(2L);
            return category;
        });

        var response = catalogService.createCategory(request);

        assertEquals(2L, response.getCategoryId());
        assertEquals("Toán luyện thi", response.getName());
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, org.mockito.Mockito.times(2)).save(categoryCaptor.capture());
        assertEquals("SUBJECT", categoryCaptor.getAllValues().get(0).getName());
        assertEquals("Toán luyện thi", categoryCaptor.getAllValues().get(1).getName());
    }

    private CatalogRequest.UpsertCategoryRequest categoryRequest(
            String name,
            String rootName,
            Long parentId,
            String status) {

        CatalogRequest.UpsertCategoryRequest request = new CatalogRequest.UpsertCategoryRequest();
        request.setName(name);
        request.setDescription("Danh mục IT");
        request.setRootName(rootName);
        request.setParentId(parentId);
        request.setStatus(status);
        return request;
    }

    private UpsertFaqRequest faqRequest(String question, String answer, String category, Boolean published) {
        UpsertFaqRequest request = new UpsertFaqRequest();
        request.setQuestion(question);
        request.setAnswer(answer);
        request.setCategory(category);
        request.setPublished(published);
        request.setSortOrder(1);
        return request;
    }

    private Category category(
            Long id,
            String name,
            CategoryType type,
            Category parent,
            String status) {

        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setType(type);
        category.setParent(parent);
        category.setStatus(status);
        category.setActive("ACTIVE".equals(status));
        category.setDescription("Danh mục " + name);
        return category;
    }

    private FaqEntry faq(
            Long id,
            String question,
            String answer,
            String category,
            Integer sortOrder,
            Boolean published) {

        FaqEntry entry = new FaqEntry();
        entry.setFaqId(id);
        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setCategory(category);
        entry.setSortOrder(sortOrder);
        entry.setPublished(published);
        return entry;
    }

    private String readSecurityConfigSource() throws IOException {
        return Files.readString(Path.of("src/main/java/com/tcs/config/SecurityConfig.java"));
    }
}
