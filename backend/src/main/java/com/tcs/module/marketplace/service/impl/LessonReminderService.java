package com.tcs.module.marketplace.service.impl;

import com.tcs.module.identity.entity.User;
import com.tcs.module.marketplace.entity.Lesson;
import com.tcs.module.marketplace.entity.TutoringClass;
import com.tcs.module.marketplace.enums.AttendanceStatus;
import com.tcs.module.marketplace.repository.LessonRepository;
import com.tcs.module.messaging.enums.NotificationType;
import com.tcs.module.messaging.service.NotificationDispatchService;
import com.tcs.module.profile.entity.Tutor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thông báo hệ thống nhắc nhở buổi học trong ngày.
 *
 * <p>Nhắc nhở tồn tại đúng trong ngày có buổi học (00:00 → hết ngày), là thông báo hệ thống
 * riêng cho gia sư và người học của buổi đó. Mỗi lần làm mới sẽ:
 * <ol>
 *   <li>Gỡ các nhắc nhở không còn thuộc hôm nay (ngày đã qua hoặc buổi đã bị đổi lịch).</li>
 *   <li>Tạo nhắc nhở cho các buổi học hôm nay chưa được nhắc.</li>
 * </ol>
 * Chạy đúng 00:00 khi chuyển ngày, và chạy lại khi khởi động ứng dụng để đảm bảo nhắc nhở
 * của hôm nay luôn hiển thị (kể cả khi server không chạy vào lúc 00:00).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonReminderService {

    /** Đánh dấu để nhận diện & dọn các thông báo nhắc nhở buổi học. */
    private static final String REF_TYPE = "LESSON_REMINDER";
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonRepository lessonRepository;
    private final NotificationDispatchService notificationDispatchService;

    @PersistenceContext
    private EntityManager em;

    /** 00:00 mỗi ngày khi chuyển ngày. */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void onDayChange() {
        refreshLessonReminders();
    }

    /** Khi khởi động: bù nhắc nhở cho hôm nay nếu server chưa chạy lúc 00:00. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        refreshLessonReminders();
    }

    private void refreshLessonReminders() {
        LocalDate today = LocalDate.now();
        // 1) Gỡ nhắc nhở không còn thuộc hôm nay (ngày cũ hoặc buổi đã đổi sang ngày khác).
        clearRemindersNotForToday(today);

        // 2) Gửi nhắc nhở cho buổi học hôm nay chưa được nhắc.
        List<Lesson> lessons = lessonRepository.findByLessonDateAndReminderSentAtIsNull(today);
        int sent = 0;
        for (Lesson lesson : lessons) {
            if (lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED
                    || lesson.getAttendanceStatus() == AttendanceStatus.ABSENT) {
                lesson.setReminderSentAt(LocalDateTime.now());
                continue;
            }
            try {
                remindOne(lesson);
                lesson.setReminderSentAt(LocalDateTime.now());
                sent++;
            } catch (Exception ex) {
                log.warn("[LessonReminder] Khong gui duoc nhac nho lesson id={}: {}",
                        lesson.getLessonId(), ex.getMessage());
            }
        }
        if (sent > 0) {
            log.info("[LessonReminder] Da gui nhac nho cho {} buoi hoc ngay {}", sent, today);
        }
    }

    /**
     * Gửi nhắc nhở NGAY cho một buổi học nếu buổi đó diễn ra hôm nay và chưa được nhắc.
     * Dùng khi đổi lịch được duyệt và buổi được chuyển sang đúng ngày hôm nay — không phải
     * chờ tới 00:00 hôm sau.
     */
    @Transactional
    public void sendReminderIfToday(Lesson lesson) {
        if (lesson == null || lesson.getLessonDate() == null) {
            return;
        }
        if (!LocalDate.now().equals(lesson.getLessonDate())) {
            return;
        }
        if (lesson.getAttendanceStatus() == AttendanceStatus.COMPLETED
                || lesson.getAttendanceStatus() == AttendanceStatus.ABSENT) {
            return;
        }
        if (lesson.getReminderSentAt() != null) {
            return; // đã nhắc rồi, tránh trùng
        }
        try {
            remindOne(lesson);
            lesson.setReminderSentAt(LocalDateTime.now());
            lessonRepository.save(lesson);
            log.info("[LessonReminder] Da gui nhac nho ngay cho buoi id={} (doi lich sang hom nay)",
                    lesson.getLessonId());
        } catch (Exception ex) {
            log.warn("[LessonReminder] Khong gui duoc nhac nho ngay lesson id={}: {}",
                    lesson.getLessonId(), ex.getMessage());
        }
    }

    /** Xóa các thông báo nhắc nhở mà buổi học tương ứng không diễn ra hôm nay. */
    private void clearRemindersNotForToday(LocalDate today) {
        String staleFilter = "reference_type = :ref AND (reference_id IS NULL OR reference_id NOT IN "
                + "(SELECT lesson_id FROM lessons WHERE lesson_date = :today))";
        em.createNativeQuery(
                "DELETE FROM notification_queues WHERE notification_id IN "
                        + "(SELECT notification_id FROM notifications WHERE " + staleFilter + ")")
                .setParameter("ref", REF_TYPE).setParameter("today", today).executeUpdate();
        int removed = em.createNativeQuery("DELETE FROM notifications WHERE " + staleFilter)
                .setParameter("ref", REF_TYPE).setParameter("today", today).executeUpdate();
        if (removed > 0) {
            log.info("[LessonReminder] Da tat {} nhac nho khong con thuoc hom nay", removed);
        }
    }

    private void remindOne(Lesson lesson) {
        TutoringClass cls = lesson.getTutoringClass();
        String classTitle = cls != null ? cls.getTitle() : "";
        String subjectName = lesson.getSlot() != null && lesson.getSlot().getSubject() != null
                ? lesson.getSlot().getSubject().getSubjectName()
                : classTitle;
        String startTime = lesson.getSlot() != null && lesson.getSlot().getStartTime() != null
                ? lesson.getSlot().getStartTime().format(HM) : "";
        String endTime = lesson.getSlot() != null && lesson.getSlot().getEndTime() != null
                ? lesson.getSlot().getEndTime().format(HM) : "";

        Map<String, Object> vars = Map.of(
                "subjectName", subjectName,
                "startTime", startTime,
                "endTime", endTime,
                "classTitle", classTitle);
        String title = "Nhắc nhở buổi học hôm nay";
        String content = "Hôm nay bạn có buổi học môn " + subjectName
                + (startTime.isEmpty() ? "" : " lúc " + startTime + " - " + endTime)
                + " (lớp \"" + classTitle + "\"). Vui lòng chuẩn bị và tham gia đúng giờ.";

        Long lessonId = lesson.getLessonId();

        // Gửi cho gia sư
        Tutor tutor = lesson.getTutor();
        if (tutor != null && tutor.getUser() != null) {
            notificationDispatchService.notifyUserFromTemplate(
                    tutor.getUser(), NotificationType.SYSTEM, "LESSON_REMINDER",
                    vars, title, content, REF_TYPE, lessonId);
        }
        // Gửi cho người học (người tạo lớp)
        User client = cls != null ? cls.getCreator() : null;
        if (client != null) {
            notificationDispatchService.notifyUserFromTemplate(
                    client, NotificationType.SYSTEM, "LESSON_REMINDER",
                    vars, title, content, REF_TYPE, lessonId);
        }
    }
}
