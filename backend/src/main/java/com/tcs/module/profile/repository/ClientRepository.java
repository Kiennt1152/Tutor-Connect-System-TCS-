package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByPhone(String phone);

    Optional<Client> findByUser_UserId(Long userId);
}
