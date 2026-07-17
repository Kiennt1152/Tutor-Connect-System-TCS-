package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.ScheduleSlot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    List<ScheduleSlot> findByTutoringClass_ClassId(Long classId);
}
