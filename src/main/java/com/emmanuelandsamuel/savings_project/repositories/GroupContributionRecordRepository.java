package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupContributionRecordRepository extends JpaRepository<GroupContributionRecord, UUID> {
    List<GroupContributionRecord> findByGroupIdAndCycleNumberAndCurrentIndex(UUID groupId, int cycleNumber, int currentIndex);
   Optional<GroupContributionRecord> findByGroupIdAndCycleNumberAndUserEmailAndCurrentIndex(UUID groupId, int cycleNumber, String userEmail, int curentIndex);

}
