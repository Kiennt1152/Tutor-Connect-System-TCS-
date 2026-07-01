package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.ParentChildLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentChildLinkRepository extends JpaRepository<ParentChildLink, Long> {

    java.util.List<ParentChildLink> findByParentUser_UserId(Long userId);
}
