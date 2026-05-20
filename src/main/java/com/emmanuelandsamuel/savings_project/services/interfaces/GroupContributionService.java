package com.emmanuelandsamuel.savings_project.services.interfaces;

import java.util.UUID;

public interface GroupContributionService {
    String payContribution(UUID groupId);
    boolean processNextCycle(UUID groupId);

}
