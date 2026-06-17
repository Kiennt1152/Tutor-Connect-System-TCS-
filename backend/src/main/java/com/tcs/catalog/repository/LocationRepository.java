package com.tcs.catalog.repository;

import com.tcs.catalog.entity.Location;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {
}
