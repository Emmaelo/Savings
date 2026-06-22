package com.emmanuelandsamuel.savings_project.services.interfaces;

import com.emmanuelandsamuel.savings_project.dtos.requests.AddMemberRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.GroupRequest;

public interface GroupService {
    String createGroup(GroupRequest groupRequest);
    String userJoinGroup(String groupNameCode);
    String userLeaveGroup(String groupNameCode);
    String activateGroup(String groupNameCode);
    String deleteGroup(String groupNameCode);
    String addMemberToGroup(AddMemberRequest request);

}
