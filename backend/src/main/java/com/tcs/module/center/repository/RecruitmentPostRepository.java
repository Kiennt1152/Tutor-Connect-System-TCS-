package com.tcs.module.center.repository;

import com.tcs.module.center.entity.RecruitmentPost;
import com.tcs.module.center.enums.RecruitmentPostStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentPostRepository extends JpaRepository<RecruitmentPost, Long> {

    /** Tin của một trung tâm (mọi trạng thái), mới nhất trước. */
    List<RecruitmentPost> findByCenter_CenterIdOrderByCreatedAtDesc(Long centerId);

    /** Tin đang mở để gia sư xem, mới đăng trước. */
    List<RecruitmentPost> findByStatusOrderByPublishedAtDesc(RecruitmentPostStatus status);
}
