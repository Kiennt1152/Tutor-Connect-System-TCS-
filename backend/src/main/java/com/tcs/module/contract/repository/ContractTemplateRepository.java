package com.tcs.module.contract.repository;

import com.tcs.module.contract.entity.ContractTemplate;
import com.tcs.module.contract.enums.ContractTemplateStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {

    List<ContractTemplate> findByStatus(ContractTemplateStatus status);

    /** Mẫu một trung tâm có thể dùng: mẫu hệ thống (center null) + mẫu của chính trung tâm đó. */
    @Query("SELECT t FROM ContractTemplate t "
            + "WHERE t.status <> com.tcs.module.contract.enums.ContractTemplateStatus.ARCHIVED "
            + "AND (t.center IS NULL OR t.center.centerId = :centerId) "
            + "ORDER BY t.defaultTemplate DESC, t.name ASC")
    List<ContractTemplate> findUsableByCenter(@Param("centerId") Long centerId);
}
