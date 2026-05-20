package com.emmanuelandsamuel.savings_project.dtos.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;
    @NotBlank(message = "Group savings type is required")
    private String groupSavingsType;
    @NotNull(message = "Member count is required")
    private int memberCount;
    @NotNull
    private BigDecimal amountToSave;
    private String guaranteeRequired; // "true" or "false" as string, will be converted to boolean in service layer
}
