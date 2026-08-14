package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {

    List<TutoringClass> findByStatus(TutoringClassStatus status);

    List<TutoringClass> findByCreator_UserId(Long userId);

    boolean existsByCategory_CategoryId(Long categoryId);

    long countByStatus(TutoringClassStatus status);

    long countByStatusIn(Collection<TutoringClassStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TutoringClass> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TutoringClass> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, org.springframework.data.domain.Pageable pageable);
}
