package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.ParentChildLink;
import com.tcs.module.profile.enums.ParentChildLinkStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentChildLinkRepository extends JpaRepository<ParentChildLink, Long> {

    List<ParentChildLink> findByParentUser_UserId(Long userId);

    List<ParentChildLink> findByParentUser_UserIdAndStatus(Long userId, ParentChildLinkStatus status);

    boolean existsByParentUser_UserIdAndChildProfile_ChildProfileIdAndStatus(
            Long userId, Long childProfileId, ParentChildLinkStatus status);

    Optional<ParentChildLink> findFirstByChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
            String fullName, LocalDate dateOfBirth, ParentChildLinkStatus status);

    Optional<ParentChildLink> findFirstByParentUser_UserIdAndChildProfile_FullNameAndChildProfile_DateOfBirthAndStatus(
            Long parentUserId, String fullName, LocalDate dateOfBirth, ParentChildLinkStatus status);
}
