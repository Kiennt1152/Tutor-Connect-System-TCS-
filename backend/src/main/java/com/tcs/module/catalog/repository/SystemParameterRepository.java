package com.tcs.module.catalog.repository;

import com.tcs.module.catalog.entity.SystemParameter;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Long> {

    Optional<SystemParameter> findByParamKey(String paramKey);
    List<SystemParameter> findByParamKeyStartingWith(String prefix);
}
