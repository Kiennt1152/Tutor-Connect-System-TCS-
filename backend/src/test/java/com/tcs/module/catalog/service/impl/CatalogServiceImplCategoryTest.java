package com.tcs.module.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcs.module.catalog.dto.request.CatalogRequest;
import com.tcs.module.catalog.dto.request.UpsertFaqRequest;
import com.tcs.module.catalog.dto.response.CatalogResponse;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit test module Catalog - quan ly cay danh muc va FAQ.
 * Bam bo test case trong Report_5.1_UnitTest: cac sheet createCategory, updateCategory,
 * deleteCategory, createFaqEntry.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceImplCategoryTest {

    private static final Long PARENT_ID = 5L;
    private static final Long CATEGORY_ID = 10L;

    @Mock private SubjectRepository subjectRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private ProvinceRepository provinceRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private WardRepository wardRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private FaqEntryRepository faqEntryRepository;
    @Mock private TutoringClassRepository tutoringClassRepository;
    @Mock private CatalogMapper catalogMapper;
    @Mock private GeminiService geminiService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private CatalogServiceImpl catalogService;

    private Category parent;

    @BeforeEach
    void setUp() {
        parent = new Category();
        parent.setCategoryId(PARENT_ID);
        parent.setName("SUBJECT");
        parent.setType(CategoryType.SUBJECT);

        when(categoryRepository.findById(PARENT_ID)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(catalogMapper.toCategoryResponse(any(), anyBoolean(), anyBoolean(), anyBoolean(), anyList()))
                .thenReturn(new CatalogResponse.CategoryResponse());
    }

    private static CatalogRequest.UpsertCategoryRequest categoryRequest(
            String name, Long parentId, String status) {
        CatalogRequest.UpsertCategoryRequest request = new CatalogRequest.UpsertCategoryRequest();
        request.setName(name);
        request.setParentId(parentId);
        request.setStatus(status);
        return request;
    }

    // ========================================================================
    //  Sheet: createCategory
    // ========================================================================

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("UTCID01 (N) - dữ liệu hợp lệ, tên chưa trùng, trạng thái hợp lệ -> tạo Category")
        void utcid01_createSuccessfully() {
            when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(PARENT_ID, "Toán 9"))
                    .thenReturn(false);

            catalogService.createCategory(categoryRequest("Toán 9", PARENT_ID, "ACTIVE"));

            ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(saved.capture());
            assertEquals("Toán 9", saved.getValue().getName());
            assertEquals("ACTIVE", saved.getValue().getStatus());
            assertEquals(parent, saved.getValue().getParent());
            assertEquals(CategoryType.SUBJECT, saved.getValue().getType());
            verify(auditLogService).record(eq("CREATE_CATEGORY"), eq("Category"), any(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - request = null -> 'Thiếu dữ liệu danh mục.'")
        void utcid02_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createCategory(null));
            assertEquals("Thiếu dữ liệu danh mục.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - tên danh mục rỗng -> 'Tên danh mục là bắt buộc.'")
        void utcid03_blankName() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createCategory(categoryRequest("   ", PARENT_ID, "ACTIVE")));
            assertEquals("Tên danh mục là bắt buộc.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - tên đã tồn tại trong cùng danh mục cha -> 'Tên danh mục đã tồn tại.'")
        void utcid04_duplicateName() {
            when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(PARENT_ID, "Toán 9"))
                    .thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createCategory(categoryRequest("Toán 9", PARENT_ID, "ACTIVE")));
            assertEquals("Tên danh mục đã tồn tại.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID05 (A) - trạng thái không hợp lệ -> 'Trạng thái danh mục không hợp lệ.'")
        void utcid05_invalidStatus() {
            when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCase(PARENT_ID, "Toán 9"))
                    .thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createCategory(categoryRequest("Toán 9", PARENT_ID, "DELETED")));
            assertEquals("Trạng thái danh mục không hợp lệ.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }
    }

    // ========================================================================
    //  Sheet: updateCategory
    // ========================================================================

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        private Category category;

        @BeforeEach
        void givenExistingCategory() {
            category = new Category();
            category.setCategoryId(CATEGORY_ID);
            category.setName("Toán 8");
            category.setType(CategoryType.SUBJECT);
            category.setParent(parent);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        }

        @Test
        @DisplayName("UTCID01 (N) - dữ liệu hợp lệ, danh mục cha khác chính nó -> cập nhật và ghi audit")
        void utcid01_updateSuccessfully() {
            when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCaseAndCategoryIdNot(
                    PARENT_ID, "Toán 9", CATEGORY_ID)).thenReturn(false);

            catalogService.updateCategory(CATEGORY_ID, categoryRequest("Toán 9", PARENT_ID, "ACTIVE"));

            assertEquals("Toán 9", category.getName());
            assertEquals("ACTIVE", category.getStatus());
            verify(categoryRepository).save(category);
            verify(auditLogService).record(
                    eq("UPDATE_CATEGORY"), eq("Category"), eq(CATEGORY_ID), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - request = null -> 'Thiếu dữ liệu danh mục.'")
        void utcid02_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.updateCategory(CATEGORY_ID, null));
            assertEquals("Thiếu dữ liệu danh mục.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - chọn chính nó làm danh mục cha -> chặn")
        void utcid03_selfAsParent() {
            when(categoryRepository.existsByParent_CategoryIdAndNameIgnoreCaseAndCategoryIdNot(
                    CATEGORY_ID, "Toán 9", CATEGORY_ID)).thenReturn(false);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.updateCategory(
                            CATEGORY_ID, categoryRequest("Toán 9", CATEGORY_ID, "ACTIVE")));
            assertEquals("Danh mục không thể tự chọn chính nó làm danh mục cha.", ex.getMessage());
            verify(categoryRepository, never()).save(any());
        }
    }

    // ========================================================================
    //  Sheet: deleteCategory
    // ========================================================================

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        private Category category;

        @BeforeEach
        void givenExistingCategory() {
            category = new Category();
            category.setCategoryId(CATEGORY_ID);
            category.setName("Toán 8");
            category.setType(CategoryType.SUBJECT);
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        }

        @Test
        @DisplayName("UTCID01 (N) - không còn danh mục con và không lớp nào dùng -> xoá và ghi audit")
        void utcid01_deleteSuccessfully() {
            when(categoryRepository.existsByParent_CategoryId(CATEGORY_ID)).thenReturn(false);
            when(tutoringClassRepository.existsByCategory_CategoryId(CATEGORY_ID)).thenReturn(false);

            catalogService.deleteCategory(CATEGORY_ID);

            verify(categoryRepository).delete(category);
            verify(auditLogService).record(
                    eq("DELETE_CATEGORY"), eq("Category"), eq(CATEGORY_ID), any(), eq(null));
        }

        @Test
        @DisplayName("UTCID02 (A) - vẫn còn danh mục con -> chặn xoá")
        void utcid02_stillHasChildren() {
            when(categoryRepository.existsByParent_CategoryId(CATEGORY_ID)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.deleteCategory(CATEGORY_ID));
            assertEquals("Không thể xóa danh mục vẫn còn danh mục con.", ex.getMessage());
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - đang được dùng cho lớp học -> chặn xoá")
        void utcid03_usedByClass() {
            when(categoryRepository.existsByParent_CategoryId(CATEGORY_ID)).thenReturn(false);
            when(tutoringClassRepository.existsByCategory_CategoryId(CATEGORY_ID)).thenReturn(true);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.deleteCategory(CATEGORY_ID));
            assertEquals("Không thể xóa danh mục đang được dùng cho lớp học.", ex.getMessage());
            verify(categoryRepository, never()).delete(any());
        }
    }

    // ========================================================================
    //  Sheet: createFaqEntry
    // ========================================================================

    @Nested
    @DisplayName("createFaqEntry")
    class CreateFaqEntry {

        private UpsertFaqRequest faqRequest(String question, String answer) {
            UpsertFaqRequest request = new UpsertFaqRequest();
            request.setQuestion(question);
            request.setAnswer(answer);
            request.setCategory("Thanh toán");
            return request;
        }

        @Test
        @DisplayName("UTCID01 (N) - có đủ câu hỏi và câu trả lời -> lưu FAQ mới")
        void utcid01_createSuccessfully() {
            when(faqEntryRepository.save(any(FaqEntry.class))).thenAnswer(inv -> inv.getArgument(0));

            catalogService.createFaqEntry(faqRequest("Làm sao để nạp tiền?", "Vào mục Ví của tôi."));

            ArgumentCaptor<FaqEntry> saved = ArgumentCaptor.forClass(FaqEntry.class);
            verify(faqEntryRepository).save(saved.capture());
            assertEquals("Làm sao để nạp tiền?", saved.getValue().getQuestion());
            assertEquals("Vào mục Ví của tôi.", saved.getValue().getAnswer());
            verify(auditLogService).record(eq("CREATE_FAQ"), eq("FaqEntry"), any(), any(), any());
        }

        @Test
        @DisplayName("UTCID02 (A) - request = null -> 'Thiếu dữ liệu FAQ.'")
        void utcid02_nullRequest() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createFaqEntry(null));
            assertEquals("Thiếu dữ liệu FAQ.", ex.getMessage());
            verify(faqEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID03 (A) - thiếu câu hỏi -> 'Câu hỏi là bắt buộc.'")
        void utcid03_missingQuestion() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createFaqEntry(faqRequest("   ", "Vào mục Ví của tôi.")));
            assertEquals("Câu hỏi là bắt buộc.", ex.getMessage());
            verify(faqEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("UTCID04 (A) - thiếu câu trả lời -> 'Câu trả lời là bắt buộc.'")
        void utcid04_missingAnswer() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> catalogService.createFaqEntry(faqRequest("Làm sao để nạp tiền?", null)));
            assertEquals("Câu trả lời là bắt buộc.", ex.getMessage());
            verify(faqEntryRepository, never()).save(any());
        }
    }
}
