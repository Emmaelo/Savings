package com.emmanuelandsamuel.savings_project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;

import com.emmanuelandsamuel.savings_project.dtos.requests.AddMemberRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.GroupRequest;
import com.emmanuelandsamuel.savings_project.services.interfaces.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "APIs for group management")
public class GroupController {

    private final GroupService groupService;


    @Operation(summary = "Create group")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<String>> createGroup(@RequestBody GroupRequest groupRequest) {
        return ResponseEntity.ok().body(new ApiResponse<>(true, groupService.createGroup(groupRequest)));
    }

  
    @Operation(summary = "Join group")
    @PostMapping("/join/{groupNameCode}")
    public ResponseEntity<ApiResponse<String>> userJoinGroup(@PathVariable String groupNameCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true,groupService.userJoinGroup(groupNameCode)));
    }

    @Operation(summary = "Leave group")
    @PostMapping("/leave/{groupNameCode}")
    public ResponseEntity<ApiResponse<String>> userLeaveGroup(@PathVariable String groupNameCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true,groupService.userLeaveGroup(groupNameCode)));
    }

   @Operation(summary = "activate group")
    @PostMapping("/activate/{groupNameCode}")
    public ResponseEntity<ApiResponse<String>> activateGroup(@PathVariable String groupNameCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true,groupService.activateGroup(groupNameCode)));
    }

    
    @Operation(summary = "Delete group")
    @PostMapping("/delete/{groupNameCode}")
    public ResponseEntity<ApiResponse<String>> deleteGroup(@PathVariable String groupNameCode) {
        return ResponseEntity.ok().body(new ApiResponse<>(true,groupService.deleteGroup(groupNameCode)));
    }

     @Operation(summary = "Add member to a group")
    @PostMapping("/add/member")
    public ResponseEntity<ApiResponse<String>> addMemberToGroup(@RequestBody AddMemberRequest request){
        return ResponseEntity.ok().body(new ApiResponse<>(true,groupService.addMemberToGroup(request)));

    }
}
