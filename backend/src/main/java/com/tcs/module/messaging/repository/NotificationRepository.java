package com.tcs.module.messaging.repository;

import com.tcs.module.messaging.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    java.util.List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    java.util.List<Notification> findByUser_UserIdAndReferenceTypeOrderByCreatedAtDesc(
            Long userId, String referenceType);

    java.util.Optional<Notification> findByNotificationIdAndUser_UserId(Long notificationId, Long userId);
}
