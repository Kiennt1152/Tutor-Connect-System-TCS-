package com.tcs.module.platform.repository;

import com.tcs.module.platform.entity.CircumventionEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CircumventionEventRepository extends JpaRepository<CircumventionEvent, Long> {
    Page<CircumventionEvent> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    Page<CircumventionEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
