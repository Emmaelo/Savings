package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.Group;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID>,JpaSpecificationExecutor<Group> {
    boolean existsByGroupCode(String groupCode);
    Optional<Group> findByGroupCode(String groupCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Group g WHERE g.groupCode = :groupCode")
    Optional<Group> findByGroupCodeForUpdate(String groupCode);
}
