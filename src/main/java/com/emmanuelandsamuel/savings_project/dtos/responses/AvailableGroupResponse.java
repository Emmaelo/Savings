package com.emmanuelandsamuel.savings_project.dtos.responses;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableGroupResponse {
    private String groupName;
    private String groupCode;
    private BigDecimal amountToSave;
    private int memberCount;
    private int currentMembers;
    private boolean guaranteeRequired;
    private String groupSavingsType;
}
