package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.Client;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByUser_UserId(Long userId);

    Optional<Client> findFirstByFullNameAndDateOfBirth(String fullName, LocalDate dateOfBirth);

    List<Client> findByDateOfBirthAfter(LocalDate dateOfBirth);

    List<Client> findByUser_UserIdIn(Collection<Long> userIds);
}
