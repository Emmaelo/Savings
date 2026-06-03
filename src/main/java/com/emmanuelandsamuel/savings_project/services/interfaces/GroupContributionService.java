package com.emmanuelandsamuel.savings_project.services.interfaces;

import java.util.UUID;

public interface GroupContributionService {
    String payContribution(String groupCode);
    String processNextCycle(UUID groupId);

}
