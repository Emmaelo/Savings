package com.emmanuelandsamuel.savings_project.dtos.responses;

import java.util.Map;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class FundWalletResponse {
    private Long amount;
    private String email;
    private Map<String, Object> metadata;

}
