package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.FaqEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqEntryRepository extends JpaRepository<FaqEntry, Long> {
}
