package com.emmanuelandsamuel.savings_project.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emmanuelandsamuel.savings_project.services.interfaces.GroupContributionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/group-contributions")
@Tag(name = "Group Contributions", description = "Endpoints for managing group contributions")
public class GroupContributionController {

    private final GroupContributionService groupContributionService;

    @Operation(summary = "Pay contribution for a group", description = "Allows a member to pay their contribution for a specific group")
   @PostMapping("/pay/{groupCode}")
    public String payContribution(String groupCode) {
        return groupContributionService.payContribution(groupCode);
    }

    @Operation(summary = "Process next cycle for a group", description = "Processes the next saving cycle for a specific group")
    @PostMapping("/next-cycle/{groupId}")
    public String processNextCycle(UUID groupId) {
        return groupContributionService.processNextCycle(groupId);
    }

}
