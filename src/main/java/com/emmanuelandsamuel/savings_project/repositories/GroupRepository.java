package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    boolean existsByGroupCode(String groupCode);
    Optional<Group> findByGroupCode(String groupCode);
}
