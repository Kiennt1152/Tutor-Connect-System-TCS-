package com.tcs.module.marketplace.service.impl;

import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dọn dẹp lớp OPEN (chưa ký hợp đồng) đã hết hạn hiển thị 30 ngày (expires_at &lt; now).
 * Hard-delete lớp cùng các đơn ứng tuyển đang chờ và dữ liệu phụ thuộc an toàn.
 * Lớp đã ghép/đang học không bị đụng tới (status khác OPEN, expires_at đã null).
 */
@Slf4j
@Service
public class ExpiredClassCleanupService {

    private final TutoringClassRepository tutoringClassRepository;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager em;

    public ExpiredClassCleanupService(TutoringClassRepository tutoringClassRepository,
                                      PlatformTransactionManager transactionManager) {
        this.tutoringClassRepository = tutoringClassRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Chạy mỗi giờ; quét lớp OPEN quá hạn và xóa. */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredOpenClasses() {
        List<TutoringClass> expired = tutoringClassRepository
                .findByStatusAndExpiresAtBefore(TutoringClassStatus.OPEN, LocalDateTime.now());
        if (expired.isEmpty()) {
            return;
        }
        log.info("[ExpiredClassCleanup] Tim thay {} lop OPEN qua han, bat dau xoa", expired.size());
        int deleted = 0;
        for (TutoringClass c : expired) {
            Long classId = c.getClassId();
            try {
                transactionTemplate.executeWithoutResult(status -> deleteClassCascade(classId));
                deleted++;
            } catch (Exception ex) {
                log.warn("[ExpiredClassCleanup] Khong xoa duoc class id={}: {}", classId, ex.getMessage());
            }
        }
        log.info("[ExpiredClassCleanup] Da xoa {}/{} lop OPEN qua han", deleted, expired.size());
    }

    /**
     * Xóa lớp OPEN cùng dữ liệu phụ thuộc theo thứ tự an toàn FK.
     * Lớp OPEN chưa từng được ghép nên không có assignment/contract/lesson.
     */
    private void deleteClassCascade(Long classId) {
        // Lịch sử trạng thái đơn ứng tuyển -> đơn ứng tuyển
        em.createNativeQuery(
                "DELETE FROM application_status_histories WHERE application_id IN "
                        + "(SELECT application_id FROM tutor_applications WHERE class_id = :id)")
                .setParameter("id", classId).executeUpdate();
        em.createNativeQuery("DELETE FROM tutor_applications WHERE class_id = :id")
                .setParameter("id", classId).executeUpdate();
        // Log gợi ý & khung lịch (nếu có)
        em.createNativeQuery("DELETE FROM recommendation_logs WHERE class_id = :id")
                .setParameter("id", classId).executeUpdate();
        em.createNativeQuery("DELETE FROM schedule_slots WHERE class_id = :id")
                .setParameter("id", classId).executeUpdate();
        // Ticket hỗ trợ tham chiếu lớp -> gỡ tham chiếu (cột nullable)
        em.createNativeQuery("UPDATE support_tickets SET target_class_id = NULL WHERE target_class_id = :id")
                .setParameter("id", classId).executeUpdate();
        // Cuối cùng xóa lớp
        em.createNativeQuery("DELETE FROM tutoring_classes WHERE class_id = :id")
                .setParameter("id", classId).executeUpdate();
    }
}
