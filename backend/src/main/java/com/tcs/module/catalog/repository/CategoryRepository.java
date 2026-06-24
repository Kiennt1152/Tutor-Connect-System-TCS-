package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, Long categoryId);

    boolean existsByParent_CategoryId(Long parentCategoryId);

    Optional<Category> findByNameIgnoreCase(String name);
}
