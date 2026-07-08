package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Location;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    java.util.List<Location> findByProvince_ProvinceId(Long provinceId);

    Optional<Location> findFirstByAddressLineIgnoreCase(String addressLine);
}
