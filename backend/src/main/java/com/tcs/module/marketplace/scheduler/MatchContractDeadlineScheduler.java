package com.tcs.module.marketplace.scheduler;

import com.tcs.module.contract.entity.Contract;
import com.tcs.module.contract.repository.ContractRepository;
import com.tcs.module.contract.repository.ContractSignatureRepository;
import com.tcs.module.finance.entity.EscrowTransaction;
import com.tcs.module.finance.enums.EscrowStatus;
import com.tcs.module.finance.repository.EscrowTransactionRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassAssignment;
import com.tcs.module.marketplace.entity.TutorApplication;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.TutorApplicationStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.TutorApplicationRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng hồ 48 giờ của hợp đồng lớp riêng (PRIVATE).
 *
 * <p>Khi client chọn gia sư, lớp vào MATCHED và bắt đầu đếm ngược 48 giờ. Trong thời gian đó các
 * gia sư ứng tuyển khác KHÔNG bị loại — họ nằm ở danh sách chờ. Hết 48 giờ mà hợp đồng chưa được
 * hai bên ký xong và tiền ký quỹ chưa vào hệ thống thì hợp đồng bị xóa: phân công chuyển DECLINED,
 * đơn của gia sư được chọn quay về chờ (SUBMITTED) cùng các gia sư khác, lớp trả về OPEN kèm học
 * phí/nội dung gốc cho đúng nhóm gia sư đã ứng tuyển từ trước.
 *
 * <p>Đã chuyển tiền (escrow khác PENDING) thì KHÔNG hủy — chỉ gỡ đồng hồ để gia sư bấm nhận lớp.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchContractDeadlineScheduler {

    private final TutoringClassRepository tutoringClassRepository;
    private final TutorApplicationRepository tutorApplicationRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final ContractRepository contractRepository;
    private final ContractSignatureRepository contractSignatureRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final NotificationDispatchService notificationDispatchService;

    /** Khớp hạn hiển thị lớp OPEN ở {@code MarketplaceServiceImpl}. */
    private static final long CLASS_DISPLAY_DAYS = 30;

    /** Rà mỗi 5 phút để đồng hồ đếm ngược ngoài giao diện và trạng thái thật không lệch nhau lâu. */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cancelExpiredMatchContracts() {
        List<TutoringClass> overdue = tutoringClassRepository.findByStatusAndMatchDeadlineAtBefore(
                TutoringClassStatus.MATCHED, LocalDateTime.now());
        if (overdue.isEmpty()) {
            return;
        }
        int cancelled = 0;
        for (TutoringClass c : overdue) {
            try {
                if (cancelOne(c)) {
                    cancelled++;
                }
            } catch (RuntimeException ex) {
                log.warn("[MatchDeadline] Khong huy duoc ghep lop id={}: {}", c.getClassId(), ex.getMessage());
            }
        }
        if (cancelled > 0) {
            log.info("[MatchDeadline] Da huy {} hop dong qua han 48h va mo lai lop", cancelled);
        }
    }

    /** @return true nếu thực sự hủy ghép và mở lại lớp. */
    private boolean cancelOne(TutoringClass c) {
        // Bám theo đơn ĐANG được chọn chứ không theo phân công mới nhất: lớp mở lại nhiều lần sẽ
        // có nhiều phân công cũ, lấy nhầm bản ghi là hủy oan lượt ghép hiện tại.
        TutorApplication accepted = tutorApplicationRepository
                .findByTutoringClass_ClassId(c.getClassId())
                .stream()
                .filter(a -> a.getStatus() == TutorApplicationStatus.ACCEPTED)
                .findFirst()
                .orElse(null);
        ClassAssignment assignment = accepted == null
                ? null
                : classAssignmentRepository
                        .findByApplication_ApplicationId(accepted.getApplicationId())
                        .orElse(null);

        // Phân công đã được xử lý (gia sư đã nhận lớp hoặc đã bị hủy) -> chỉ gỡ đồng hồ.
        if (assignment == null || assignment.getStatus() != ClassAssignmentStatus.PENDING) {
            clearDeadline(c);
            return false;
        }

        // Tiền đã vào hệ thống thì hợp đồng vẫn có hiệu lực, chỉ còn chờ gia sư bấm nhận lớp.
        EscrowTransaction escrow = escrowTransactionRepository
                .findByAssignment_AssignmentId(assignment.getAssignmentId())
                .orElse(null);
        if (escrow != null && escrow.getStatus() != EscrowStatus.PENDING) {
            log.info("[MatchDeadline] Lop {} da co tien ky quy (escrow {}), giu hop dong",
                    c.getClassId(), escrow.getStatus());
            clearDeadline(c);
            return false;
        }

        TutorApplication chosen = assignment.getApplication();
        assignment.setStatus(ClassAssignmentStatus.DECLINED);
        // Xóa dấu ký cũ: chọn lại đúng gia sư này sẽ dùng lại phân công, phải ký lại từ đầu.
        assignment.setTutorSignedAt(null);
        assignment.setClientSignedAt(null);
        classAssignmentRepository.save(assignment);

        deleteContract(assignment.getAssignmentId());

        // Gia sư cũ KHÔNG bị loại: đơn quay về chờ, nằm cùng các gia sư khác để phụ huynh chọn lại.
        if (chosen != null) {
            chosen.setStatus(TutorApplicationStatus.SUBMITTED);
            chosen.setReviewedAt(null);
            tutorApplicationRepository.save(chosen);
        }

        // Trả lớp về đúng nội dung/học phí trước khi áp giá của gia sư vừa bị hủy.
        if (c.getPreMatchDetailsJson() != null) {
            c.setDetailsJson(c.getPreMatchDetailsJson());
        }
        if (c.getPreMatchTuitionFee() != null) {
            c.setTuitionFee(c.getPreMatchTuitionFee());
        }
        c.setPreMatchDetailsJson(null);
        c.setPreMatchTuitionFee(null);
        c.setStatus(TutoringClassStatus.OPEN);
        c.setMatchDeadlineAt(null);
        // Lớp coi như được đăng lại: nếu hạn hiển thị 30 ngày đã trôi qua trong lúc chờ ký thì
        // gia hạn, không thì job dọn lớp hết hạn sẽ xóa mất lớp vừa mở lại.
        if (c.getExpiresAt() == null || c.getExpiresAt().isBefore(LocalDateTime.now())) {
            c.setExpiresAt(LocalDateTime.now().plusDays(CLASS_DISPLAY_DAYS));
        }
        tutoringClassRepository.save(c);

        notifyCancelled(c, chosen);
        return true;
    }

    /** Gỡ hạn 48 giờ của lớp (khi không còn gì để huỷ). */
    private void clearDeadline(TutoringClass c) {
        c.setMatchDeadlineAt(null);
        tutoringClassRepository.save(c);
    }

    /**
     * Hợp đồng chưa có hiệu lực (chưa có tiền ký quỹ) của phân công bị hủy -> xóa hẳn cùng chữ ký.
     * Mỗi phân công chỉ có một hợp đồng, nên để lại bản cũ thì lần chọn lại gia sư này sẽ dùng nhầm.
     */
    private void deleteContract(Long assignmentId) {
        Contract contract = contractRepository.findByAssignment_AssignmentId(assignmentId).orElse(null);
        if (contract == null) {
            return;
        }
        contractSignatureRepository.deleteAll(
                contractSignatureRepository.findByContract_ContractId(contract.getContractId()));
        contractRepository.delete(contract);
    }

    /** Báo khi hợp đồng hết hạn 48 giờ: chủ lớp, gia sư đã chọn và các gia sư trong danh sách chờ (lớp đã mở lại). */
    private void notifyCancelled(TutoringClass c, TutorApplication chosen) {
        String title = "Hợp đồng hết hạn 48 giờ - lớp đã mở lại";

        if (c.getCreator() != null) {
            notify(c, c.getCreator(), title,
                    "Lớp \"" + c.getTitle() + "\" chưa hoàn tất ký hợp đồng và thanh toán ký quỹ trong"
                            + " 48 giờ nên hệ thống đã hủy ghép gia sư. Lớp được mở lại và các gia sư đã"
                            + " ứng tuyển trước đó vẫn còn trong danh sách để bạn chọn lại.");
        }

        if (chosen != null && chosen.getTutor() != null && chosen.getTutor().getUser() != null) {
            notify(c, chosen.getTutor().getUser(), title,
                    "Hợp đồng lớp \"" + c.getTitle() + "\" đã hết hiệu lực do quá 48 giờ mà chưa ký xong"
                            + " và chưa có thanh toán ký quỹ. Lớp đã được mở lại; đơn ứng tuyển của bạn vẫn"
                            + " còn trong danh sách chờ và có thể được chọn lại.");
        }

        Long chosenId = chosen != null ? chosen.getApplicationId() : null;
        for (TutorApplication app : tutorApplicationRepository.findByTutoringClass_ClassId(c.getClassId())) {
            if (app.getApplicationId().equals(chosenId)
                    || app.getStatus() != TutorApplicationStatus.SUBMITTED
                    || app.getTutor() == null
                    || app.getTutor().getUser() == null) {
                continue;
            }
            notify(c, app.getTutor().getUser(), "Lớp bạn ứng tuyển đã mở lại",
                    "Lớp \"" + c.getTitle() + "\" đã mở lại do hợp đồng với gia sư được chọn hết hạn"
                            + " 48 giờ. Đơn ứng tuyển của bạn vẫn còn hiệu lực và đang chờ phụ huynh/học"
                            + " viên xem lại.");
        }
    }

    /** Gửi một thông báo loại APPLICATION về lớp cho một người. */
    private void notify(TutoringClass c, User user, String title, String content) {
        notificationDispatchService.notifyUserFromTemplate(
                user,
                NotificationType.APPLICATION,
                "MARKETPLACE_CLASS_EVENT",
                Map.of("title", title, "content", content),
                title,
                content,
                "TUTORING_CLASS",
                c.getClassId());
    }
}
