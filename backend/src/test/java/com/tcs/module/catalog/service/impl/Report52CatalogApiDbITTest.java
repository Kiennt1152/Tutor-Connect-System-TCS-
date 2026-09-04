package com.tcs.module.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcs.exception.GlobalExceptionHandler;
import com.tcs.module.catalog.controller.CatalogController;
import com.tcs.module.catalog.entity.Category;
import com.tcs.module.catalog.entity.FaqEntry;
import com.tcs.module.catalog.enums.CategoryType;
import com.tcs.module.catalog.mapper.CatalogMapper;
import com.tcs.module.catalog.repository.CategoryRepository;
import com.tcs.module.catalog.repository.FaqEntryRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.controller.MessagingController;
import com.tcs.module.platform.service.AuditLogService;
import com.tcs.module.catalog.service.GeminiService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@Tag("report52-it")
@SpringBootTest(
        classes = Report52CatalogApiDbITTest.TestApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:tcs_catalog_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never"
        })
class Report52CatalogApiDbITTest {

    private MockMvc mockMvc;

    @jakarta.annotation.Resource
    private CatalogController catalogController;

    @jakarta.annotation.Resource
    private CategoryRepository categoryRepository;

    @jakarta.annotation.Resource
    private FaqEntryRepository faqEntryRepository;

    @BeforeEach
    void setUpMockMvcAndCleanDatabase() {
        mockMvc = MockMvcBuilders.standaloneSetup(catalogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        faqEntryRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        faqEntryRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void IT_CAT_001_LoadCategoryTreeWithParentChildHierarchyForCatalogFormsThroughApiAndDb() throws Exception {
        Category root = category(null, "SUBJECT", CategoryType.SUBJECT, null, "ACTIVE");
        root = categoryRepository.saveAndFlush(root);
        Category math = category(null, "Toán", CategoryType.SUBJECT, root, "ACTIVE");
        Category english = category(null, "Tiếng Anh", CategoryType.SUBJECT, root, "INACTIVE");
        categoryRepository.saveAndFlush(math);
        categoryRepository.saveAndFlush(english);

        mockMvc.perform(get("/api/catalog/categories").param("root", "SUBJECT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("SUBJECT"))
                .andExpect(jsonPath("$[0].children.length()").value(2))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"Toán\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"Tiếng Anh\"")));

        Category reloadedRoot = categoryRepository.findById(root.getCategoryId()).orElseThrow();
        assertEquals("SUBJECT", reloadedRoot.getName());
    }

    @Test
    void IT_CAT_002_FilterPublishedFaqByCategoryAndVietnameseKeywordThroughApiAndDb() throws Exception {
        faqEntryRepository.saveAndFlush(faq(null, "Thanh toán học phí như thế nào?", "Phụ huynh quét mã QR để thanh toán.", "PAYMENT", 1, true));
        faqEntryRepository.saveAndFlush(faq(null, "Cập nhật hồ sơ gia sư", "Vào trang xác minh để cập nhật.", "PROFILE", 2, true));
        faqEntryRepository.saveAndFlush(faq(null, "FAQ nháp", "Chưa xuất bản.", "PAYMENT", 3, false));

        mockMvc.perform(get("/api/catalog/faq")
                        .param("category", "PAYMENT")
                        .param("keyword", "thanh toan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].question").value("Thanh toán học phí như thế nào?"))
                .andExpect(jsonPath("$[0].category").value("PAYMENT"));

        assertEquals(3, faqEntryRepository.count());
        assertTrue(faqEntryRepository.findByPublishedTrueOrderBySortOrderAscFaqIdAsc().size() >= 1);
    }

    private Category category(Long id, String name, CategoryType type, Category parent, String status) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setName(name);
        category.setType(type);
        category.setParent(parent);
        category.setStatus(status);
        category.setActive("ACTIVE".equalsIgnoreCase(status));
        return category;
    }

    private FaqEntry faq(Long id, String question, String answer, String category, Integer sortOrder, boolean published) {
        FaqEntry faqEntry = new FaqEntry();
        faqEntry.setFaqId(id);
        faqEntry.setQuestion(question);
        faqEntry.setAnswer(answer);
        faqEntry.setCategory(category);
        faqEntry.setSortOrder(sortOrder);
        faqEntry.setPublished(published);
        return faqEntry;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.tcs")
    @EnableJpaRepositories(basePackageClasses = {CategoryRepository.class, FaqEntryRepository.class})
    @Import({
            CatalogController.class,
            CatalogServiceImpl.class,
            TestBeans.class
    })
    static class TestApplication {
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        CatalogMapper catalogMapper() {
            return new CatalogMapper();
        }

        @Bean
        TutoringClassRepository tutoringClassRepository() {
            return mock(TutoringClassRepository.class);
        }

        @Bean
        GeminiService geminiService() {
            return mock(GeminiService.class);
        }

        @Bean
        AuditLogService auditLogService() {
            return mock(AuditLogService.class);
        }
    }
}
