package com.tcs.module.profile.repository;

import com.tcs.module.profile.entity.MediaFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByUploadedBy_UserIdOrderByCreatedAtDesc(Long userId);
}
