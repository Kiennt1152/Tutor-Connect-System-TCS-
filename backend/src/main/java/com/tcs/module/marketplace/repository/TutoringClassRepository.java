package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.TutoringClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {
}
