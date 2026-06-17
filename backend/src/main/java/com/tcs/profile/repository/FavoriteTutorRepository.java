package com.tcs.profile.repository;

import com.tcs.profile.entity.FavoriteTutor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteTutorRepository extends JpaRepository<FavoriteTutor, UUID> {
}
