package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Ward;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {

    List<Ward> findByDistrictIdOrderByWardName(Long districtId);
}
