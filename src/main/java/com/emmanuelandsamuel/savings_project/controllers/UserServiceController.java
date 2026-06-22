package com.emmanuelandsamuel.savings_project.controllers;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emmanuelandsamuel.savings_project.dtos.requests.BankAccountRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.GroupSearchRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.BankListResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.PageResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.UserGroupResponse;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserService;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserWalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Self service")
@RequestMapping("/api/v1/user")
public class UserServiceController {

    private final UserWalletService userWalletService;
    private final UserService userService;

    @Operation(summary = "Add Bank Account")
    @PostMapping("/addAccout")
    public  ResponseEntity<ApiResponse<String>>  addBankAccount(@RequestBody BankAccountRequest bankAccountRequest){
        return ResponseEntity.ok().body(new ApiResponse<>(true, userWalletService.addBankAccount(bankAccountRequest)));
    }

    @Operation(summary = "Add or update secret pin")
    @PostMapping("/{pin}")
    public ResponseEntity<ApiResponse<String>>  addSecretPin( @Valid @PathVariable String pin){
         return ResponseEntity.ok().body(new ApiResponse<>(true, userWalletService.addSecretPin(pin)));
    }

    // This is not working yet
     @Operation(summary = "Search for groups to join")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<Object>>> searchJoinableGroups( @RequestBody GroupSearchRequest request){
        Page<Group> page = userService.searchJoinableGroups(request);
        PageResponse<Object> pageResponse = PageResponse.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
        return ResponseEntity.ok().body(new ApiResponse<>(true, "success", pageResponse));
    }

    @Operation(summary = "Return logged in user Banks detail")
    @GetMapping("/bank")
    public ResponseEntity<ApiResponse<List<BankListResponse>>> getUsersBanks(){
        return ResponseEntity.ok().body(new ApiResponse<>(true, "success", userService.getUsersBanks()));
    }

     @Operation(summary = "Return groups logged in user is a member in")
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<UserGroupResponse>>> getUsersGroups(){
        return ResponseEntity.ok().body(new ApiResponse<>(true, "success", userService.getUsersGroups()));
    }



     @Operation(summary = "User search group Code")
    @GetMapping("/{groupCode}")
      public ResponseEntity<ApiResponse<UserGroupResponse>> findGroupByCode(@PathVariable String groupCode){
        return ResponseEntity.ok().body(new ApiResponse<>(true, "", userService.findGroupByCode(groupCode)));
      }

}
