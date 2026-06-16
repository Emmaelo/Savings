package com.emmanuelandsamuel.savings_project.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserGroupResponse {

    private String groupName;

    private String groupCode;

    private BigDecimal amountToSave;

    private String groupStatus;

    private int currentCycle;

    private int payoutIndex;

    private boolean paidCurrentCycle;

    private LocalDate nextContributionDate;

    private String creatorEmail;

    private String groupSavingsType;

    private List<Data> data;

    private int memberCount;

    private int currentMemberCount;

    private boolean guaranteeRequired;


    @lombok.Data
    public static class Data{
        private String memberEmail;
        private String hasMemberPaid;
        private LocalDate datePaid;

    }

}
