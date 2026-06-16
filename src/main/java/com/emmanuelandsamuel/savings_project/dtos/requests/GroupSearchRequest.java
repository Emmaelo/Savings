package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.math.BigDecimal;

import com.emmanuelandsamuel.savings_project.enumerations.GroupSavingsType;

import lombok.Data;

@Data
public class GroupSearchRequest {
    
    private Boolean guaranteeRequired;

    private GroupSavingsType groupSavingsType;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private int page = 0;

}
