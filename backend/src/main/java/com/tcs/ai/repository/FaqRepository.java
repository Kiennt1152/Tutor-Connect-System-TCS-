package com.tcs.ai.repository;

import com.tcs.ai.entity.Faq;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {
}
