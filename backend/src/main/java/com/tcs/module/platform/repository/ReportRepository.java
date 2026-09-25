package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status);

    List<Report> findByTargetTypeOrderByCreatedAtDesc(ReportTargetType targetType);

    /** Các báo cáo nhắm vào một đối tượng (ví dụ một đánh giá), mới nhất trước. */
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            ReportTargetType targetType, Long targetId);

    /**
     * Tìm các báo cáo có đính kèm đúng URL của một file private.
     *
     * <p>URL được lưu trong cột TEXT dưới dạng danh sách phân tách bởi dấu phẩy,
     * xuống dòng hoặc dấu chấm phẩy nên cần tìm theo chuỗi chứa. Quyền truy cập
     * vẫn phải được kiểm tra thêm theo loại đối tượng và chủ sở hữu lớp.</p>
     */
    List<Report> findByTargetTypeAndEvidenceUrlsContaining(
            ReportTargetType targetType, String evidenceUrl);

    boolean existsByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status);

    long countByStatus(ReportStatus status);

    List<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status);

    long countByReporter_UserIdAndCreatedAtAfter(Long reporterUserId, LocalDateTime after);

    List<Report> findAllByOrderByCreatedAtDesc();
}
