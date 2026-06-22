package com.emmanuelandsamuel.savings_project.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMemberRequest {
    private String groupCode;
    private String emailOrNumber;

}
