package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByParentIsNullAndNameIgnoreCase(String name);

    boolean existsByParentIsNullAndNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);

    boolean existsByParent_CategoryIdAndNameIgnoreCase(Long parentCategoryId, String name);

    boolean existsByParent_CategoryIdAndNameIgnoreCaseAndCategoryIdNot(
            Long parentCategoryId,
            String name,
            Long categoryId
    );

    boolean existsByParent_CategoryId(Long parentCategoryId);

    Optional<Category> findByNameIgnoreCase(String name);
}
