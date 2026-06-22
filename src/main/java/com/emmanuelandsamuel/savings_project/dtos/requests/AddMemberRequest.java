package com.emmanuelandsamuel.savings_project.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMemberRequest {
     @NotBlank(message = "Group Code is required")
    private String groupCode;
 
     @NotBlank(message = "Email or Phone number is required")
    private String emailOrNumber;

}
