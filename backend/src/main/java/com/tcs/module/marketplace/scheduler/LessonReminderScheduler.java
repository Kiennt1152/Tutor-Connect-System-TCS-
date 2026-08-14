package com.tcs.module.marketplace.scheduler;

import com.tcs.module.catalog.entity.SystemParameter;
import com.tcs.module.catalog.repository.SystemParameterRepository;
import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.ClassStudent;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.ClassAssignmentStatus;
import com.tcs.module.marketplace.enums.ClassStudentStatus;
import com.tcs.module.marketplace.enums.TutoringClassStatus;
import com.tcs.module.marketplace.repository.ClassAssignmentRepository;
import com.tcs.module.marketplace.repository.ClassStudentRepository;
import com.tcs.module.marketplace.repository.ScheduleSlotRepository;
import com.tcs.module.marketplace.repository.TutoringClassRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bước 13 (BF-04) / bước 10 (BF-05): hệ thống gửi nhắc nhở buổi học cho các bên (UC-37).
 *
 * <p>Mỗi giờ quét các lớp đang chạy có buổi học diễn ra NGÀY MAI, gửi thông báo cho
 * gia sư phụ trách và phụ huynh của từng học viên. Mỗi lớp chỉ nhắc MỘT LẦN cho mỗi ngày
 * (chống trùng bằng cờ trong system_parameters — không cần bảng/migration mới).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonReminderScheduler {

    private static final long ONE_HOUR = 3_600_000L;

    private final TutoringClassRepository tutoringClassRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ClassStudentRepository classStudentRepository;
    private final ClassAssignmentRepository classAssignmentRepository;
    private final SystemParameterRepository systemParameterRepository;
    private final NotificationDispatchService notificationDispatchService;

    @Scheduled(fixedRate = ONE_HOUR)
    @Transactional
    public void sendUpcomingLessonReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int weekday = tomorrow.getDayOfWeek().getValue();

        for (TutoringClass c : tutoringClassRepository.findAll()) {
            if (!isActive(c) || outOfRange(c, tomorrow)) {
                continue;
            }
            boolean hasSessionTomorrow = scheduleSlotRepository
                    .findByTutoringClass_ClassId(c.getClassId()).stream()
                    .anyMatch(s -> s.getDayOfWeek() != null && s.getDayOfWeek() == weekday);
            if (!hasSessionTomorrow) {
                continue;
            }

            String dedupeKey = "lessonreminder:" + c.getClassId() + ":" + tomorrow;
            if (systemParameterRepository.findByParamKey(dedupeKey).isPresent()) {
                continue; // đã nhắc lớp này cho ngày mai rồi
            }

            String title = "Nhắc buổi học ngày mai";
            String content = "Lớp \"" + c.getTitle() + "\" có buổi học vào ngày " + tomorrow
                    + ". Vui lòng chuẩn bị và tham gia đúng giờ.";

            // Gia sư phụ trách lớp.
            classAssignmentRepository
                    .findFirstByApplication_TutoringClass_ClassIdAndStatus(
                            c.getClassId(), ClassAssignmentStatus.ACTIVE)
                    .filter(a -> a.getTutor() != null && a.getTutor().getUser() != null)
                    .ifPresent(a -> notify(a.getTutor().getUser(), title, content, c.getClassId()));

            // Phụ huynh của từng học viên đã ghi danh.
            List<ClassStudent> students = classStudentRepository
                    .findByTutoringClass_ClassIdAndStatus(c.getClassId(), ClassStudentStatus.ENROLLED);
            for (ClassStudent s : students) {
                if (s.getEnrolledByUser() != null) {
                    notify(s.getEnrolledByUser(), title, content, c.getClassId());
                }
            }

            SystemParameter mark = systemParameterRepository.findByParamKey(dedupeKey)
                    .orElseGet(SystemParameter::new);
            mark.setParamKey(dedupeKey);
            mark.setParamValue("1");
            mark.setDescription("Đã gửi nhắc buổi học");
            systemParameterRepository.save(mark);
            log.info("[LessonReminder] Đã gửi nhắc buổi học ngày {} cho lớp {}", tomorrow, c.getClassId());
        }
    }

    private void notify(User user, String title, String content, Long classId) {
        notificationDispatchService.notifyUserFromTemplate(
                user,
                NotificationType.SYSTEM,
                "LESSON_REMINDER",
                Map.of("title", title, "content", content),
                title,
                content,
                "CLASS_ACTIVE",
                classId);
    }

    private boolean isActive(TutoringClass c) {
        TutoringClassStatus st = c.getStatus();
        return st == TutoringClassStatus.IN_PROGRESS
                || st == TutoringClassStatus.MATCHED
                || st == TutoringClassStatus.ENROLLMENT_CLOSED;
    }

    private boolean outOfRange(TutoringClass c, LocalDate date) {
        return c.getStartDate() == null || c.getEndDate() == null
                || date.isBefore(c.getStartDate()) || date.isAfter(c.getEndDate());
    }
}
