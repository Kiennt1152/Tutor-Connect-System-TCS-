package com.tcs.trust.repository;

import com.tcs.trust.entity.Report;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
}
