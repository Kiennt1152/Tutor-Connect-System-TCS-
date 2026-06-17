package com.tcs.trust.repository;

import com.tcs.trust.entity.Dispute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
}
