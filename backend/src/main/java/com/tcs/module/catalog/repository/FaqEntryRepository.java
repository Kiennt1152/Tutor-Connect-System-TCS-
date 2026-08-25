package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.FaqEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

    // =========================================================================
    // LUỒNG 1: TRA CỨU & TÌM KIẾM FAQ TRI THỨC (UC-61, UC-67)
    // =========================================================================

    // Luồng 1 - Truy vấn JPQL tìm kiếm FAQ công khai (published = true) theo category và keyword
    @Query("""
            SELECT f FROM FaqEntry f
            WHERE f.published = true
            AND (:category IS NULL OR :category = '' OR f.category = :category)
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY f.sortOrder ASC, f.faqId ASC
            """)
    List<FaqEntry> search(@Param("category") String category, @Param("keyword") String keyword);

    // Luồng 1 - Lấy toàn bộ danh sách FAQ đã xuất bản (is_published = true)
    List<FaqEntry> findByPublishedTrueOrderBySortOrderAscFaqIdAsc();

    // Luồng 1 - Lấy FAQ đã xuất bản theo danh mục chỉ định
    List<FaqEntry> findByPublishedTrueAndCategoryOrderBySortOrderAscFaqIdAsc(String category);

    // =========================================================================
    // LUỒNG 6: QUẢN TRỊ FAQ - ADMIN TÌM KIẾM CẢ BẢN NHÁP CHƯA XUẤT BẢN (UC-67)
    // =========================================================================
    @Query("""
            SELECT f FROM FaqEntry f
            WHERE (:category IS NULL OR :category = '' OR f.category = :category)
            AND (
                :keyword IS NULL OR :keyword = '' OR
                LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(f.answer) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY f.sortOrder ASC, f.faqId ASC
            """)
    List<FaqEntry> searchAdmin(@Param("category") String category, @Param("keyword") String keyword);
}
