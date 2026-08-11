package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
