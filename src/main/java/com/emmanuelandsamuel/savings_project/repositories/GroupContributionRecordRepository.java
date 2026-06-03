package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupContributionRecordRepository extends JpaRepository<GroupContributionRecord, UUID> {
    List<GroupContributionRecord> findByGroupIdAndCycleNumber(UUID groupId, int cycleNumber);
   Optional<GroupContributionRecord> findByGroupIdAndCycleNumberAndUserId(UUID groupId, int cycleNumber, UUID userId);

}
