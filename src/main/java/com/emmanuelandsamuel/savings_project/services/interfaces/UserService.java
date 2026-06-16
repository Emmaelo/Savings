package com.emmanuelandsamuel.savings_project.services.interfaces;

import java.util.List;

import org.springframework.data.domain.Page;

import com.emmanuelandsamuel.savings_project.dtos.requests.GroupSearchRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.BankListResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.UserGroupResponse;
import com.emmanuelandsamuel.savings_project.entities.Group;

public interface UserService {
    List<BankListResponse> getUsersBanks();
    List<UserGroupResponse> getUsersGroups();
    Page<Group> searchJoinableGroups(GroupSearchRequest request);
    UserGroupResponse findGroupByCode(String groupCode);

}
