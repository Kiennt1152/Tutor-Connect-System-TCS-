package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.TutorBusyTime;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutorBusyTimeRepository extends JpaRepository<TutorBusyTime, Long> {

    /** Lịch bận của một gia sư trong một khoảng ngày (thường là trọn một tháng). */
    List<TutorBusyTime> findByTutor_TutorIdAndBusyDateBetweenOrderByBusyDateAscStartTimeAsc(
            Long tutorId, LocalDate from, LocalDate to);

    /** Lịch bận sẵn có trên các ngày sắp đăng ký — để chặn trùng giờ. */
    List<TutorBusyTime> findByTutor_TutorIdAndBusyDateIn(Long tutorId, Collection<LocalDate> dates);
}
