package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.Province;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Long> {

    /** Tìm theo tên người dùng tự nhập (province_name là UNIQUE). */
    Optional<Province> findFirstByProvinceNameIgnoreCase(String provinceName);
}
