package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.Report;
import com.tcs.module.platform.enums.ReportStatus;
import com.tcs.module.platform.enums.ReportTargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporter_UserIdAndTargetTypeAndTargetIdAndStatusOrderByCreatedAtDesc(
            Long reporterUserId,
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status);
}
