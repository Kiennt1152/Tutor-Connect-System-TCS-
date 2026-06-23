package com.tcs.module.marketplace.repository;

import com.tcs.module.marketplace.entity.MatchingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchingPreferenceRepository extends JpaRepository<MatchingPreference, Long> {
}
