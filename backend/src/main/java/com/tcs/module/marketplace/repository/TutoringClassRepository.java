package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassType;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {

    List<TutoringClass> findByStatus(TutoringClassStatus status);

    List<TutoringClass> findByStatusIn(java.util.Collection<TutoringClassStatus> statuses);

    List<TutoringClass> findByCreator_UserId(Long userId);
    /** Lớp của một trung tâm theo trạng thái, mới tạo trước — dùng cho trang hồ sơ trung tâm. */
    List<TutoringClass> findByCenter_CenterIdAndStatusOrderByCreatedAtDesc(
            Long centerId, TutoringClassStatus status);

    /**
     * Kiểm tra lớp CENTER có thuộc về tài khoản trung tâm đang đăng nhập.
     * Một số dữ liệu cũ xác định chủ sở hữu qua center.user, một số dữ liệu
     * xác định qua creator nên cần hỗ trợ cả hai quan hệ.
     */
    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
            FROM TutoringClass t
            LEFT JOIN t.center center
            WHERE t.classId = :classId
              AND t.classType = :classType
              AND (
                    center.user.userId = :centerUserId
                    OR t.creator.userId = :centerUserId
                  )
            """)
    boolean existsCenterOwnedClass(
            @Param("classId") Long classId,
            @Param("classType") ClassType classType,
            @Param("centerUserId") Long centerUserId);

    boolean existsByCategory_CategoryId(Long categoryId);

    long countByStatus(TutoringClassStatus status);

    long countByStatusIn(Collection<TutoringClassStatus> statuses);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TutoringClass> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<TutoringClass> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Lớp OPEN đã quá hạn hiển thị (expires_at < mốc truyền vào) -> cần dọn dẹp. */
    List<TutoringClass> findByStatusAndExpiresAtBefore(
            TutoringClassStatus status, LocalDateTime cutoff);
}
