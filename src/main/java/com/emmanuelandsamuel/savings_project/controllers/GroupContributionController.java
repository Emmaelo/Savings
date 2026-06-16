package com.emmanuelandsamuel.savings_project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emmanuelandsamuel.savings_project.dtos.requests.GuaranteeBalanceRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.services.interfaces.GroupContributionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/group-contributions")
@Tag(name = "Group Contributions", description = "Endpoints for managing group contributions")
public class GroupContributionController {

    private final GroupContributionService groupContributionService;

    @Operation(summary = "Pay contribution for a group", description = "Allows a member to pay their contribution for a specific group")
   @PostMapping("/pay/{groupCode}")
    public ResponseEntity<ApiResponse< String>> payContribution(String groupCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true, groupContributionService.payContribution(groupCode)));
    }

    @Operation(summary = "Process next cycle for a group", description = "Processes the next saving cycle for a specific group")
    @PostMapping("/next-cycle/{groupCode}")
    public ResponseEntity<ApiResponse< String>> processNextCycle(String groupCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true, groupContributionService.processNextCycle(groupCode)));
    }

    @Operation(summary = "Request to close Group", description = "Request to Close group after the final cycle so that you can exit group")
    @PostMapping("/vote/{groupCode}")
    public ResponseEntity<ApiResponse< String>> requestOrRemoveClosure(@PathVariable String groupCode){
        return ResponseEntity.ok().body(new ApiResponse<>(true, groupContributionService.requestOrRemoveClosure(groupCode)));
    }


    @Operation(summary = "Credit Guarantee Balance", description = "Top up the guarantee balance")
    @PostMapping("/fund/guarantee")
   public ResponseEntity<ApiResponse< String>> fundGuaranteeBalance(@RequestBody GuaranteeBalanceRequest request){
    return ResponseEntity.ok().body(new ApiResponse<>(true, groupContributionService.fundGuaranteeBalance(request)));
   }

}
