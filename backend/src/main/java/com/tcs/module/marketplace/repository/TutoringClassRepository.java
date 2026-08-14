package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {

    java.util.List<TutoringClass> findByStatus(TutoringClassStatus status);

    java.util.List<TutoringClass> findByCreator_UserId(Long userId);

    boolean existsByCategory_CategoryId(Long categoryId);

    /** Lớp OPEN đã quá hạn hiển thị (expires_at < mốc truyền vào) -> cần dọn dẹp. */
    java.util.List<TutoringClass> findByStatusAndExpiresAtBefore(
            TutoringClassStatus status, java.time.LocalDateTime cutoff);
}
