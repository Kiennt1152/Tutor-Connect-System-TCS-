package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.ChildProfile;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildProfileRepository extends JpaRepository<ChildProfile, Long> {

    Optional<ChildProfile> findFirstByFullNameAndDateOfBirth(String fullName, LocalDate dateOfBirth);
}
