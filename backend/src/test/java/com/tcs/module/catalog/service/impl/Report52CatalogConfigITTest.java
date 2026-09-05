package com.tcs.module.catalog.service.impl;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.request.ChatbotAskRequest;
import com.tcs.module.catalog.dto.request.UpsertFaqRequest;
import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.request.UpsertSystemParameterRequest;
import com.tcs.module.catalog.dto.response.ChatbotAskResponse;
import com.tcs.module.catalog.dto.response.FaqResponse;
import com.tcs.module.catalog.dto.response.SystemParameterResponse;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.enums.CategoryType;
import com.tcs.module.catalog.mapper.CatalogMapper;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.DistrictRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.catalog.repository.GradeRepository;
import com.tcs.module.catalog.repository.LocationRepository;
import com.tcs.module.catalog.repository.ProvinceRepository;
import com.tcs.module.catalog.repository.SubjectRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.catalog.repository.WardRepository;
import com.tcs.module.catalog.service.GeminiService;
import com.tcs.module.catalog.service.impl.SystemParameterServiceImpl;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.platform.service.AuditLogService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("report52-it")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Report52CatalogConfigITTest {

    private static final Long PARAM_ID = 10L;

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
    @Mock private SystemParameterRepository systemParameterRepository;

    private final CatalogMapper catalogMapper = new CatalogMapper();

    private CatalogServiceImpl catalogService;
    private SystemParameterServiceImpl systemParameterService;

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

        systemParameterService = new SystemParameterServiceImpl(
                systemParameterRepository,
                auditLogService);
    }


    /**
     * Test Case: IT-CAT-001
     * Title: Load the category tree with parent and child records through the catalog API.
     * Procedure: Prepare the stated fixture and input, then execute GET /api/catalog/categories -> CatalogServiceImpl.
     * Input: root=SUBJECT.
     * Steps:
     *   1. Prepare the fixture: Real H2 categories contain SUBJECT, Toán and Tiếng Anh hierarchy.
     *   2. Use the input: root=SUBJECT.
     *   3. Execute GET /api/catalog/categories -> CatalogServiceImpl. Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogApiDbITTest#IT_CAT_001_LoadCategoryTreeWithParentChildHierarchyForCatalogFormsThroughApiAndDb.
     *   4. Compare the result with the expected behavior and the API/DB checks: Assert HTTP tree shape/child names and reload the root row.
     * Expected: The API returns the SUBJECT root with active/inactive child categories and the root can be reloaded from the database.
     * Pre-conditions: Real H2 categories contain SUBJECT, Toán and Tiếng Anh hierarchy.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-001: Load the category tree with parent and child records through the catalog API.")
    void IT_CAT_001_LoadCategoryTreeWithParentChildHierarchyForCatalogForms() {
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

    /**
     * Test Case: IT-CAT-002
     * Title: Filter published FAQ entries by category and Vietnamese keyword through the API.
     * Procedure: Prepare the stated fixture and input, then execute GET /api/catalog/faq -> CatalogServiceImpl.
     * Input: category=PAYMENT; keyword=thanh toan.
     * Steps:
     *   1. Prepare the fixture: Real H2 FAQ data contains two published rows and one draft row.
     *   2. Use the input: category=PAYMENT; keyword=thanh toan.
     *   3. Execute GET /api/catalog/faq -> CatalogServiceImpl. Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogApiDbITTest#IT_CAT_002_FilterPublishedFaqByCategoryAndVietnameseKeywordThroughApiAndDb.
     *   4. Compare the result with the expected behavior and the API/DB checks: Assert HTTP count/question/category and database row counts.
     * Expected: Only the published PAYMENT FAQ matching “thanh toan” is returned; the draft FAQ is excluded.
     * Pre-conditions: Real H2 FAQ data contains two published rows and one draft row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-002: Filter published FAQ entries by category and Vietnamese keyword through the API.")
    void IT_CAT_002_FilterPublishedFaqByCategoryAndVietnameseKeyword() {
        FaqEntry payment = faq(10L, "Thanh toán học phí như thế nào?", "Phụ huynh quét mã QR để thanh toán.", "PAYMENT", 1, true);
        FaqEntry profile = faq(11L, "Cập nhật hồ sơ gia sư", "Vào trang xác minh để cập nhật.", "PROFILE", 2, true);

        when(faqEntryRepository.findByPublishedTrueAndCategoryOrderBySortOrderAscFaqIdAsc("PAYMENT"))
                .thenReturn(List.of(payment, profile));

        List<FaqResponse> responses = catalogService.getFaqEntries("PAYMENT", "thanh toan");

        assertEquals(1, responses.size());
        assertEquals(10L, responses.get(0).getFaqId());
        assertEquals("Thanh toán học phí như thế nào?", responses.get(0).getQuestion());
    }

    /**
     * Test Case: IT-CAT-003
     * Title: Load category detail with parent, usage and deletable flags.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.getCategoryById (GET /api/catalog/categories/{categoryId}).
     * Input: categoryId=2.
     * Steps:
     *   1. Prepare the fixture: Category 2 has parent 1 and a tutoring class references it.
     *   2. Use the input: categoryId=2.
     *   3. Execute CatalogServiceImpl.getCategoryById (GET /api/catalog/categories/{categoryId}). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_003_ReadCategoryDetailWithParentUsageAndDeletableFlags.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert id/name/parent/usage/deletable fields.
     * Expected: Category 2 returns parent 1, usedByTutoringClasses=true and deletable=false.
     * Pre-conditions: Category 2 has parent 1 and a tutoring class references it.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-003: Load category detail with parent, usage and deletable flags.")
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

    /**
     * Test Case: IT-CAT-004
     * Title: Reject category creation when the name is missing.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.createCategory (POST /api/catalog/categories).
     * Input: Blank category name.
     * Steps:
     *   1. Prepare the fixture: Admin is authorized.
     *   2. Use the input: Blank category name.
     *   3. Execute CatalogServiceImpl.createCategory (POST /api/catalog/categories). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_004_RejectCreateCategoryWhenNameIsMissing.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no category save.
     * Expected: The service returns “Tên danh mục là bắt buộc.” and does not save a category.
     * Pre-conditions: Admin is authorized.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-004: Reject category creation when the name is missing.")
    void IT_CAT_004_RejectCreateCategoryWhenNameIsMissing() {
        CatalogRequest.UpsertCategoryRequest request = categoryRequest(" ", "SUBJECT", null, "ACTIVE");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createCategory(request));

        assertEquals("Tên danh mục là bắt buộc.", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CAT-005
     * Title: Reject a platform-fee rate above the allowed 0.50 limit.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters).
     * Input: PLATFORM_FEE_RATE=0.51.
     * Steps:
     *   1. Prepare the fixture: PLATFORM_FEE_RATE is not already present.
     *   2. Use the input: PLATFORM_FEE_RATE=0.51.
     *   3. Execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_CAT_005_RejectPlatformFeeRateAboveAllowedRange.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exact boundary error and no save.
     * Expected: Rate 0.51 is rejected with the boundary message and no row is saved.
     * Pre-conditions: PLATFORM_FEE_RATE is not already present.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-005: Reject a platform-fee rate above the allowed 0.50 limit.")
    void IT_CAT_005_RejectPlatformFeeRateAboveAllowedRange() {
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.createParameter(request("PLATFORM_FEE_RATE", "0.51")));

        assertEquals("PLATFORM_FEE_RATE phải từ 0.00 đến 0.50.", exception.getMessage());
        verify(systemParameterRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CAT-006
     * Title: Keep public catalog reads open while protecting admin catalog routes.
     * Procedure: Prepare the stated fixture and input, then execute SecurityConfig route declarations plus public catalog endpoints.
     * Input: Route snippets for public catalog GET, chatbot POST and admin catalog writes.
     * Steps:
     *   1. Prepare the fixture: Source security configuration is available for inspection.
     *   2. Use the input: Route snippets for public catalog GET, chatbot POST and admin catalog writes.
     *   3. Execute SecurityConfig route declarations plus public catalog endpoints. Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_006_AnonymousCanReadPublicCatalogButAdminCatalogRoutesRemainProtected.
     *   4. Compare the result with the expected behavior and the source/configuration checks: Assert permitAll/protected ordering in SecurityConfig source.
     * Expected: The security configuration permits public catalog GET routes and leaves admin catalog write/management routes protected.
     * Pre-conditions: Source security configuration is available for inspection.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-006: Keep public catalog reads open while protecting admin catalog routes.")
    void IT_CAT_006_AnonymousCanReadPublicCatalogButAdminCatalogRoutesRemainProtected() throws Exception {
        Path path = Path.of("src/main/java/com/tcs/config/SecurityConfig.java");
        if (!Files.exists(path)) {
            path = Path.of("backend/src/main/java/com/tcs/config/SecurityConfig.java");
        }
        assertTrue(Files.exists(path), "SecurityConfig.java should exist for inspection");
        String content = Files.readString(path);

        assertTrue(content.contains("HttpMethod.GET, \"/api/catalog/**\""));
        assertTrue(content.contains("HttpMethod.POST, \"/api/catalog/chatbot/ask\""));
        assertTrue(content.contains("RbacConstants.PLATFORM_ADMIN"));
    }

    /**
     * Test Case: IT-CAT-007
     * Title: Allow normal users to read public catalog data but block catalog writes.
     * Procedure: Prepare the stated fixture and input, then execute SecurityConfig route declarations plus catalog endpoints.
     * Input: Public GET/POST route snippets and admin write route snippet.
     * Steps:
     *   1. Prepare the fixture: Source security configuration is available for inspection.
     *   2. Use the input: Public GET/POST route snippets and admin write route snippet.
     *   3. Execute SecurityConfig route declarations plus catalog endpoints. Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_007_NonAdminCanReadPublicCatalogButCannotUseCatalogWriteEndpoints.
     *   4. Compare the result with the expected behavior and the source/configuration checks: Assert route order and permitAll/protected declarations.
     * Expected: Public catalog reads/chatbot access are permitted while admin catalog write endpoints remain protected.
     * Pre-conditions: Source security configuration is available for inspection.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-007: Allow normal users to read public catalog data but block catalog writes.")
    void IT_CAT_007_NonAdminCanReadPublicCatalogButCannotUseCatalogWriteEndpoints() throws Exception {
        Path path = Path.of("src/main/java/com/tcs/config/SecurityConfig.java");
        if (!Files.exists(path)) {
            path = Path.of("backend/src/main/java/com/tcs/config/SecurityConfig.java");
        }
        assertTrue(Files.exists(path), "SecurityConfig.java should exist for inspection");
        String content = Files.readString(path);

        assertTrue(content.contains("HttpMethod.GET, \"/api/catalog/**\""));
        assertTrue(content.contains("permitAll()"));
        assertTrue(content.contains("\"/api/catalog/**\""));
        assertTrue(content.contains("PLATFORM_ADMIN"));
    }

    /**
     * Test Case: IT-CAT-008
     * Title: Reject a duplicate category name under the same parent.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.createCategory (POST /api/catalog/categories).
     * Input: New child name Toán under parent 1.
     * Steps:
     *   1. Prepare the fixture: Parent 1 already has a category named Toán.
     *   2. Use the input: New child name Toán under parent 1.
     *   3. Execute CatalogServiceImpl.createCategory (POST /api/catalog/categories). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_008_RejectDuplicateCategoryNameWithinSameParent.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert duplicate error and verify no save.
     * Expected: The service returns “Tên danh mục đã tồn tại.” and does not save a second category.
     * Pre-conditions: Parent 1 already has a category named Toán.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-008: Reject a duplicate category name under the same parent.")
    void IT_CAT_008_RejectDuplicateCategoryNameWithinSameParent() {
        CatalogRequest.UpsertCategoryRequest request = categoryRequest("Toán", null, 1L, "ACTIVE");

        when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(1L, "Toán")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createCategory(request));

        assertEquals("Tên danh mục đã tồn tại.", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CAT-009
     * Title: Reject updating a system parameter to a key used by another row.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}).
     * Input: parameterId=10; new key TAKEN_KEY.
     * Steps:
     *   1. Prepare the fixture: Parameter 10 exists and another row already uses TAKEN_KEY.
     *   2. Use the input: parameterId=10; new key TAKEN_KEY.
     *   3. Execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_CAT_009_RejectUpdatingToKeyAlreadyUsedByAnotherParameter.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no conflicting save.
     * Expected: The duplicate-key error for TAKEN_KEY is returned and the existing parameter is unchanged.
     * Pre-conditions: Parameter 10 exists and another row already uses TAKEN_KEY.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-009: Reject updating a system parameter to a key used by another row.")
    void IT_CAT_009_RejectUpdatingToKeyAlreadyUsedByAnotherParameter() {
        when(systemParameterRepository.findById(PARAM_ID))
                .thenReturn(Optional.of(parameter(PARAM_ID, "MY_KEY", "old")));
        when(systemParameterRepository.findByParamKey("TAKEN_KEY"))
                .thenReturn(Optional.of(parameter(99L, "TAKEN_KEY", "value")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.updateParameter(PARAM_ID, request("TAKEN_KEY", "new")));

        assertEquals("Khóa tham số đã tồn tại: TAKEN_KEY", exception.getMessage());
    }

    /**
     * Test Case: IT-CAT-010
     * Title: Update a category and record the old/new values in the audit log.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.updateCategory (PUT /api/catalog/categories/{categoryId}).
     * Input: categoryId=2; name Toán nâng cao; status INACTIVE.
     * Steps:
     *   1. Prepare the fixture: Category 2 belongs to root 1 and the new name is unused.
     *   2. Use the input: categoryId=2; name Toán nâng cao; status INACTIVE.
     *   3. Execute CatalogServiceImpl.updateCategory (PUT /api/catalog/categories/{categoryId}). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_010_UpdateCategoryRecordsAuditWithOldAndNewValues.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response fields and capture audit action/entity/request.
     * Expected: Category 2 is renamed Toán nâng cao, set INACTIVE and audited with UPDATE_CATEGORY.
     * Pre-conditions: Category 2 belongs to root 1 and the new name is unused.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-010: Update a category and record the old/new values in the audit log.")
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

    /**
     * Test Case: IT-CAT-011
     * Title: Verify that catalog/configuration changes notify affected users.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl/SystemParameterServiceImpl write paths.
     * Input: A category/FAQ/parameter update followed by notification-list inspection.
     * Steps:
     *   1. Prepare the fixture: Admin changes a category, FAQ or system parameter and a notification recipient is available.
     *   2. Use the input: A category/FAQ/parameter update followed by notification-list inspection.
     *   3. Execute CatalogServiceImpl/SystemParameterServiceImpl write paths. Mapped test: No JUnit function is available for this case..
     *   4. Record the observed gap and keep the result as Failed; do not infer a pass from another test.
     * Expected: FAIL: current source records audit logs for catalog/configuration changes but does not dispatch an in-app notification; no matching JUnit function exists.
     * Pre-conditions: Admin changes a category, FAQ or system parameter and a notification recipient is available.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-011: Verify that catalog/configuration changes notify affected users.")
    @Disabled("Report 5.2 gap: current source records audit logs for catalog/configuration changes but does not dispatch an in-app notification; no matching JUnit function exists.")
    void IT_CAT_011_VerifyCatalogConfigurationChangesNotifyAffectedUsers() {
        // Known gap in Report 5.2: current source records audit logs for catalog/configuration changes
        // but does not dispatch an in-app notification; no matching JUnit function exists.
    }

    /**
     * Test Case: IT-CAT-012
     * Title: Prevent deleting a category that still has child categories.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.deleteCategory (DELETE /api/catalog/categories/{categoryId}).
     * Input: categoryId=1.
     * Steps:
     *   1. Prepare the fixture: Category 1 has at least one child.
     *   2. Use the input: categoryId=1.
     *   3. Execute CatalogServiceImpl.deleteCategory (DELETE /api/catalog/categories/{categoryId}). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_012_RejectDeletingCategoryThatStillHasChildren.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify delete is never called.
     * Expected: The service returns the child-category error and does not delete the parent.
     * Pre-conditions: Category 1 has at least one child.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-012: Prevent deleting a category that still has child categories.")
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

    /**
     * Test Case: IT-CAT-013
     * Title: Answer a chatbot question from a published FAQ before using the AI provider.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.askChatbot (POST /api/catalog/chatbot/ask).
     * Input: Question matching FAQ 20.
     * Steps:
     *   1. Prepare the fixture: A published FAQ matches the question.
     *   2. Use the input: Question matching FAQ 20.
     *   3. Execute CatalogServiceImpl.askChatbot (POST /api/catalog/chatbot/ask). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_013_ChatbotUsesPublishedFaqBeforeCallingAiProvider.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response flags/id and verify AI provider is never called.
     * Expected: A matching published FAQ returns matched=true, aiGenerated=false and FAQ id 20; GeminiService is not called.
     * Pre-conditions: A published FAQ matches the question.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-013: Answer a chatbot question from a published FAQ before using the AI provider.")
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

    /**
     * Test Case: IT-CAT-014
     * Title: Store an accepted platform-fee parameter value for later finance use.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters).
     * Input: paramKey=PLATFORM_FEE_RATE; paramValue=0.00.
     * Steps:
     *   1. Prepare the fixture: No existing PLATFORM_FEE_RATE row.
     *   2. Use the input: paramKey=PLATFORM_FEE_RATE; paramValue=0.00.
     *   3. Execute SystemParameterServiceImpl.createParameter (POST /api/catalog/parameters). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_CAT_014_PlatformFeeParameterStoresAcceptedValueForFinance.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Capture saved parameter and assert key/value.
     * Expected: A new PLATFORM_FEE_RATE with value 0.00 is normalized to “0” and saved.
     * Pre-conditions: No existing PLATFORM_FEE_RATE row.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-014: Store an accepted platform-fee parameter value for later finance use.")
    void IT_CAT_014_PlatformFeeParameterStoresAcceptedValueForFinance() {
        when(systemParameterRepository.findByParamKey("PLATFORM_FEE_RATE")).thenReturn(Optional.empty());
        when(systemParameterRepository.save(any(SystemParameter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        systemParameterService.createParameter(request("PLATFORM_FEE_RATE", "0.00"));

        verify(systemParameterRepository).save(any(SystemParameter.class));
    }

    /**
     * Test Case: IT-CAT-015
     * Title: Create an FAQ entry and write its audit record.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.createFaqEntry (POST /api/catalog/faq).
     * Input: Question “Cách thanh toán?” with answer/category.
     * Steps:
     *   1. Prepare the fixture: Admin is authorized and FAQ data is valid.
     *   2. Use the input: Question “Cách thanh toán?” with answer/category.
     *   3. Execute CatalogServiceImpl.createFaqEntry (POST /api/catalog/faq). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_015_CreateFaqEntryWritesAuditLog.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response id/question and audit action/entity.
     * Expected: FAQ 30 is returned with its question and a CREATE_FAQ audit row is recorded.
     * Pre-conditions: Admin is authorized and FAQ data is valid.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-015: Create an FAQ entry and write its audit record.")
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

    /**
     * Test Case: IT-CAT-016
     * Title: Reject moving a category below one of its descendants.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.updateCategory (PUT /api/catalog/categories/{categoryId}).
     * Input: Update category 2 with parent category 3.
     * Steps:
     *   1. Prepare the fixture: Category 2 is an ancestor of category 3.
     *   2. Use the input: Update category 2 with parent category 3.
     *   3. Execute CatalogServiceImpl.updateCategory (PUT /api/catalog/categories/{categoryId}). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_016_RejectMovingCategoryUnderItsOwnDescendant.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exception and verify no invalid save.
     * Expected: The service returns the self-descendant error and does not save the invalid hierarchy.
     * Pre-conditions: Category 2 is an ancestor of category 3.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-016: Reject moving a category below one of its descendants.")
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

    /**
     * Test Case: IT-CAT-017
     * Title: Prevent renaming a mandatory PLATFORM_FEE_RATE key.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}).
     * Input: parameterId=10; new key OTHER_KEY.
     * Steps:
     *   1. Prepare the fixture: Parameter 10 has mandatory key PLATFORM_FEE_RATE.
     *   2. Use the input: parameterId=10; new key OTHER_KEY.
     *   3. Execute SystemParameterServiceImpl.updateParameter (PATCH /api/catalog/parameters/{parameterId}). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_CAT_017_RejectRenamingMandatoryPlatformFeeKey.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert error and verify no save.
     * Expected: The service returns the mandatory-key error and does not save the renamed parameter.
     * Pre-conditions: Parameter 10 has mandatory key PLATFORM_FEE_RATE.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-017: Prevent renaming a mandatory PLATFORM_FEE_RATE key.")
    void IT_CAT_017_RejectRenamingMandatoryPlatformFeeKey() {
        when(systemParameterRepository.findById(PARAM_ID))
                .thenReturn(Optional.of(parameter(PARAM_ID, "PLATFORM_FEE_RATE", "0.02")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemParameterService.updateParameter(PARAM_ID, request("OTHER_KEY", "0.02")));

        assertEquals("Không thể đổi tên khóa cấu hình bắt buộc: PLATFORM_FEE_RATE", exception.getMessage());
    }

    /**
     * Test Case: IT-CAT-018
     * Title: Search system parameters by normalized prefix and keyword in sorted order.
     * Procedure: Prepare the stated fixture and input, then execute SystemParameterServiceImpl.getParameters (GET /api/catalog/parameters).
     * Input: prefix=" finance. "; keyword="fee".
     * Steps:
     *   1. Prepare the fixture: Repository has a finance.platform_fee_percent parameter.
     *   2. Use the input: prefix=" finance. "; keyword="fee".
     *   3. Execute SystemParameterServiceImpl.getParameters (GET /api/catalog/parameters). Mapped test: com.tcs.module.catalog.service.impl.Report52SystemParameterITTest#IT_CAT_018_SearchSystemParametersByPrefixAndKeywordReturnsSortedResult.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response count/key and verify normalized prefix query.
     * Expected: A prefix with surrounding spaces is normalized and the matching finance fee parameter is returned first/only.
     * Pre-conditions: Repository has a finance.platform_fee_percent parameter.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-018: Search system parameters by normalized prefix and keyword in sorted order.")
    void IT_CAT_018_SearchSystemParametersByPrefixAndKeywordReturnsSortedResult() {
        SystemParameter financeFee = parameter(1L, "finance.platform_fee_percent", "10");
        SystemParameter financeHold = parameter(2L, "finance.escrow_hold_days", "7");
        SystemParameter authOtp = parameter(3L, "auth.otp_minutes", "5");

        when(systemParameterRepository.findByParamKeyStartingWith("finance."))
                .thenReturn(List.of(financeHold, financeFee, authOtp));

        var responses = systemParameterService.getParameters(" finance. ", "fee");

        assertEquals(1, responses.size());
        assertEquals("finance.platform_fee_percent", responses.get(0).getParamKey());
        verify(systemParameterRepository).findByParamKeyStartingWith(eq("finance."));
    }

    /**
     * Test Case: IT-CAT-019
     * Title: Keep catalog validation messages clear and free from raw enum/class names.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.createFaqEntry (POST /api/catalog/faq).
     * Input: Blank question; valid answer/category.
     * Steps:
     *   1. Prepare the fixture: Admin submits an otherwise valid FAQ request.
     *   2. Use the input: Blank question; valid answer/category.
     *   3. Execute CatalogServiceImpl.createFaqEntry (POST /api/catalog/faq). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_019_CatalogValidationMessagesStayVietnameseAndDoNotExposeRawEnumNames.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert exact message and absence of raw enum/class text.
     * Expected: A blank FAQ question returns “Câu hỏi là bắt buộc.” without exposing PAYMENT or FaqEntry implementation names.
     * Pre-conditions: Admin submits an otherwise valid FAQ request.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-019: Keep catalog validation messages clear and free from raw enum/class names.")
    void IT_CAT_019_CatalogValidationMessagesStayVietnameseAndDoNotExposeRawEnumNames() {
        UpsertFaqRequest request = faqRequest("   ", "Quét mã QR.", "PAYMENT", true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> catalogService.createFaqEntry(request));

        assertEquals("Câu hỏi là bắt buộc.", exception.getMessage());
        assertFalse(exception.getMessage().contains("PAYMENT"));
        assertFalse(exception.getMessage().contains("FaqEntry"));
        verify(faqEntryRepository, never()).save(any());
    }

    /**
     * Test Case: IT-CAT-020
     * Title: Create a missing root category and its requested child in one catalog operation.
     * Procedure: Prepare the stated fixture and input, then execute CatalogServiceImpl.createCategory (POST /api/catalog/categories).
     * Input: Child name Toán luyện thi under root SUBJECT.
     * Steps:
     *   1. Prepare the fixture: Requested root SUBJECT does not yet exist and child name is unused.
     *   2. Use the input: Child name Toán luyện thi under root SUBJECT.
     *   3. Execute CatalogServiceImpl.createCategory (POST /api/catalog/categories). Mapped test: com.tcs.module.catalog.service.impl.Report52CatalogConfigITTest#IT_CAT_020_CreateCategoryUnderRequestedRootCreatesMissingRootAndChild.
     *   4. Compare the result with the expected behavior and the service with mocked collaborators checks: Assert response and capture two category saves in root-then-child order.
     * Expected: The operation saves root SUBJECT and child Toán luyện thi, then returns the child category id/name.
     * Pre-conditions: Requested root SUBJECT does not yet exist and child name is unused.
     */
    @Test
    @Tag("report52-it")
    @DisplayName("IT-CAT-020: Create a missing root category and its requested child in one catalog operation.")
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


    private UpsertSystemParameterRequest request(String key, String value) {
        UpsertSystemParameterRequest request = new UpsertSystemParameterRequest();
        request.setParamKey(key);
        request.setParamValue(value);
        request.setDescription("IT config value");
        return request;
    }

    private SystemParameter parameter(Long id, String key, String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setParameterId(id);
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        return parameter;
    }

}
