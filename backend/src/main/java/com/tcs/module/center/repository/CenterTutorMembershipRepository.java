package com.tcs.module.center.repository;

import com.tcs.module.center.entity.CenterTutorMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenterTutorMembershipRepository extends JpaRepository<CenterTutorMembership, Long> {

    /** Danh sách gia sư của một trung tâm, mới gia nhập trước. */
    List<CenterTutorMembership> findByCenter_CenterIdOrderByJoinedAtDesc(Long centerId);

    /** Một gia sư chỉ có một membership trong một trung tâm (UNIQUE center_id, tutor_id). */
    Optional<CenterTutorMembership> findFirstByCenter_CenterIdAndTutor_TutorId(
            Long centerId, Long tutorId);
}
