package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.GuaranteeBalanceRequest;

public interface GroupContributionService {
    String payContribution(String groupCode);
    String processNextCycle(String  groupCode);
    String requestOrRemoveClosure(String groupCode);
    String fundGuaranteeBalance(GuaranteeBalanceRequest request);

}
