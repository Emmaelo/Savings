package com.emmanuelandsamuel.savings_project.services.implementations;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.emmanuelandsamuel.savings_project.dtos.requests.GroupSearchRequest;
import com.emmanuelandsamuel.savings_project.dtos.responses.BankListResponse;
import com.emmanuelandsamuel.savings_project.dtos.responses.UserGroupResponse;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.GroupMember;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.GroupMemberRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserBankAccountRepositories;
import com.emmanuelandsamuel.savings_project.services.interfaces.UserService;
import com.emmanuelandsamuel.savings_project.utilities.GroupSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImplementation implements UserService {

        private final UserBankAccountRepositories userBankAccountRepositories;
        private final GroupMemberRepository groupMemberRepository;
        private final GroupRepository groupRepository;

        public List<BankListResponse> getUsersBanks() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                return userBankAccountRepositories.findByUserEmail(email)
                                .stream()
                                .map(u -> BankListResponse.builder()
                                                .bankCode(u.getBankCode())
                                                .accountNumber(u.getAccountNumber())
                                                .bankName(u.getBankName())
                                                .build())
                                .toList();
        }

        public List<UserGroupResponse> getUsersGroups() {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                List<GroupMember> memberships = groupMemberRepository.findUserGroups(email);

                return memberships.stream()
                                .map(m -> {

                                        List<UserGroupResponse.Data> members = m.getGroup()
                                                        .getGroupMembers()
                                                        .stream()
                                                        .map(member -> {

                                                                UserGroupResponse.Data data = new UserGroupResponse.Data();

                                                                data.setMemberEmail(member.getUserEmail());

                                                                data.setHasMemberPaid(
                                                                                member.isPaidCurrentCycle()
                                                                                                ? "PAID"
                                                                                                : "NOT_PAID");

                                                                return data;
                                                        })
                                                        .toList();

                                        return UserGroupResponse.builder()
                                                        .groupName(m.getGroup().getName())
                                                        .currentMemberCount(m.getGroup().getCurrentMemberCount())
                                                        .memberCount(m.getGroup().getMemberCount())
                                                        .guaranteeRequired(m.getGroup().isGuaranteeRequired())
                                                        .paidCurrentCycle(m.isPaidCurrentCycle())
                                                        .groupCode(m.getGroupCode())
                                                        .amountToSave(m.getGroup().getAmountToSave())
                                                        .groupStatus(
                                                                        String.valueOf(
                                                                                        m.getGroup().getGroupStatus()))
                                                        .currentCycle(
                                                                        m.getGroup().getCurrentCycle())
                                                        .nextContributionDate(
                                                                        m.getGroup().getNextContributionDate())
                                                        .creatorEmail(
                                                                        m.getGroup().getCreatorEmail())
                                                        .groupSavingsType(
                                                                        String.valueOf(
                                                                                        m.getGroup()
                                                                                                        .getGroupSavingsType()))
                                                        .payoutIndex(m.getPayoutIndex())
                                                        .data(members)
                                                        .build();
                                })
                                .toList();
        }


        

        public UserGroupResponse findGroupByCode(String groupCode) {
                Group group = groupRepository.findByGroupCode(groupCode)
                                .orElseThrow(() -> new ApplicationException("Group Code not found"));

                return UserGroupResponse.builder()
                                .amountToSave(group.getAmountToSave())
                                .groupName(group.getName())
                                .memberCount(group.getMemberCount())
                                .currentMemberCount(group.getCurrentMemberCount())
                                .groupSavingsType(String.valueOf(group.getGroupSavingsType()))
                                .guaranteeRequired(group.isGuaranteeRequired())
                                .build();
        }




        public Page<Group> searchJoinableGroups(
                        GroupSearchRequest request) {

                Pageable pageable = PageRequest.of(
                                request.getPage(),
                                20,
                                Sort.by(Sort.Direction.DESC, "createdAt"));

                return groupRepository.findAll(
                                GroupSpecification.searchableGroups(request),
                                pageable);
        }

}
