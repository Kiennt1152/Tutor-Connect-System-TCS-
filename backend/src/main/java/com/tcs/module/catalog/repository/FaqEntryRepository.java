package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.FaqEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {

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

    List<FaqEntry> findByPublishedTrueOrderBySortOrderAscFaqIdAsc();
}
