package com.emmanuelandsamuel.savings_project.services.interfaces;

public interface GroupContributionService {
    String payContribution(String groupCode);
    String processNextCycle(String  groupCode);

}
