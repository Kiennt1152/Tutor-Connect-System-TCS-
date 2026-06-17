package com.tcs.profile.repository;

import com.tcs.profile.entity.Client;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
}
