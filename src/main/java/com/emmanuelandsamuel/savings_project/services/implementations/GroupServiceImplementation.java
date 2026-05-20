package com.emmanuelandsamuel.savings_project.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emmanuelandsamuel.savings_project.dtos.requests.GroupRequest;
import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord.ContributionStatus;
import com.emmanuelandsamuel.savings_project.entities.GroupMember;
import com.emmanuelandsamuel.savings_project.entities.GroupWallet;
import com.emmanuelandsamuel.savings_project.entities.GroupWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.enumerations.GroupSavingsType;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupContributionRecordRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.GroupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImplementation implements GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupWalletLedgerRepository groupWalletLedgerRepository;
    private final CompanyWalletRepository companyWalletRepository;
    private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
    private final GroupContributionRecordRepository groupContributionRecordRepository;
    private static final UUID COMPANY_WALLET_ID = UUID.fromString("0820-0000-01");
    private static final BigDecimal FEE = BigDecimal.valueOf(500);
    private final GroupContributionServiceImplementation groupContributionService;

    @Override
    @Transactional
    public String createGroup(GroupRequest groupRequest) {
        if (groupRequest.getAmountToSave().compareTo(BigDecimal.valueOf(2000)) < 0) {
            return "Amount must be at least 2000.";
        }
        if (groupRequest.getMemberCount() < 3) {
            return "Member count must be at least 3.";
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found with email: " + email));

        if (user.getUserWallet().getAvailableBalance().compareTo(groupRequest.getAmountToSave().add(FEE)) < 0) {
            return "Insufficient funds in your wallet to create this group. Please fund your wallet and try again.";
        }

        if (groupRepository.existsByName(groupRequest.getName())) {
            return groupRequest.getName() + " already exists. Please choose a different name";
        }

        GroupSavingsType groupSavingsType = GroupSavingsType.valueOf(groupRequest.getGroupSavingsType().toUpperCase());

        Group group = Group.builder()
                .name(groupRequest.getName())
                .creatorId(user.getId())
                .amountToSave(groupRequest.getAmountToSave())
                .groupSavingsType(groupSavingsType)
                .memberCount(groupRequest.getMemberCount())
                .groupStatus(GroupStatus.INACTIVE)
                .guaranteeRequired(groupRequest.getGuaranteeRequired() == "true" ? true : false)
                .groupCode(generateUniqueGroupCode(groupSavingsType))
                .build();

        group.addMember(
                GroupMember.builder()
                        .userId(user.getId())
                        .guaranteeBalance(groupRequest.getGuaranteeRequired() == "true" ? groupRequest.getAmountToSave()
                                : BigDecimal.ZERO)
                        .build());

        user.getUserWallet().setAvailableBalance(
                user.getUserWallet().getAvailableBalance().subtract(groupRequest.getAmountToSave()));
        userRepository.save(user);

        // User wallet ledger is needed here

        group.assignGroupWallet(GroupWallet.builder().build());
        group.getGroupWallet().setAvailableBalance(
                group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));
        groupRepository.save(group);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(groupRequest.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.CREDIT)
                .userId(user.getId())
                .build();
        groupWalletLedgerRepository.save(ledgerEntry);

        companyWalletRepository.findById(COMPANY_WALLET_ID).ifPresent(companyWallet -> {
            companyWallet.setAvailableBalance(companyWallet.getAvailableBalance().add(FEE));
            companyWalletRepository.save(companyWallet);

            companyWalletLedgerRepository.save(
                    CompanyWalletLedger.builder()
                            .walletId(companyWallet.getId())
                            .amount(FEE)
                            .balanceAfter(companyWallet.getAvailableBalance())
                            .entryType(LedgerEntryType.CREDIT)
                            .build());
        });

        return "Group created successfully";
    }

    @Transactional
    @Override
    public String userJoinGroup(String groupNameCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found with email: " + email));

        Optional<Group> groupOptional = groupRepository.findByGroupCodeOrName(groupNameCode, groupNameCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with name or code: " + groupNameCode;
        }
        Group group = groupOptional.get();

        if (user.getUserWallet().getAvailableBalance().compareTo(group.getAmountToSave()) < 0) {
            return "Insufficient funds in your wallet to join this group. Please fund your wallet and try again.";
        }

        if (group.getGroupMembers().stream().anyMatch(member -> member.getUserId().equals(user.getId()))) {
            return "You are already a member of this group.";
        }

        if (group.getGroupStatus() == GroupStatus.ACTIVE || group.getGroupStatus() == GroupStatus.CLOSED) {
            return "You cannot join this group at the moment.";
        }

        if (group.getGroupMembers().size() >= group.getMemberCount()) {
            return "Group is filled. You cannot join this group.";
        }

        user.getUserWallet().setAvailableBalance(
                user.getUserWallet().getAvailableBalance().subtract(group.getAmountToSave()));

        userRepository.save(user);

        group.addMember(
                GroupMember.builder()
                        .userId(user.getId())
                        .guaranteeBalance(group.isGuaranteeRequired() ? group.getAmountToSave() : BigDecimal.ZERO)
                        .build());
        group.getGroupWallet().setAvailableBalance(
                group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));

        groupRepository.save(group);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(group.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.CREDIT)
                .userId(user.getId())
                .build();

        groupWalletLedgerRepository.save(ledgerEntry);

        return "Joined Group successfully";
    }

    @Override
    @Transactional
    public String userLeaveGroup(String groupNameCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found with email: " + email));

        Optional<Group> groupOptional = groupRepository.findByGroupCodeOrName(groupNameCode, groupNameCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with name or code: " + groupNameCode;
        }
        Group group = groupOptional.get();

        if (group.getGroupStatus() == GroupStatus.ACTIVE || group.getGroupStatus() == GroupStatus.CLOSED) {
            return "You cannot leave this group at the moment.";
        }
        if (group.getCreatorId().equals(user.getId())) {
            return "As the creator of the group, you cannot leave the group. You can only delete the group.";
        }

        GroupMember member = group.getGroupMembers().stream()
                .filter(m -> m.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException("You are not a member of this group."));

        if (member.getGuaranteeBalance().compareTo(BigDecimal.ZERO) > 0) {
            user.getUserWallet().setAvailableBalance(
                    user.getUserWallet().getAvailableBalance().add(member.getGuaranteeBalance()));
        }

        if (group.getGroupWallet().getAvailableBalance().compareTo(group.getAmountToSave()) >= 0) {
            group.getGroupWallet().setAvailableBalance(
                    group.getGroupWallet().getAvailableBalance().subtract(group.getAmountToSave()));
            user.getUserWallet().setAvailableBalance(
                    user.getUserWallet().getAvailableBalance().add(group.getAmountToSave()));
        }
        group.removeMember(member);

        groupRepository.save(group);

        userRepository.save(user);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(group.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.DEBIT)
                .userId(user.getId())
                .build();

        groupWalletLedgerRepository.save(ledgerEntry);

        return "Left Group successfully";
    }

    @Override
    public String activateGroup(String groupNameCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found with email: " + email));

        Optional<Group> groupOptional = groupRepository.findByGroupCodeOrName(groupNameCode, groupNameCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with name or code: " + groupNameCode;
        }
        Group group = groupOptional.get();

        if (!group.getCreatorId().equals(user.getId())) {
            return "Only the group creator can activate the group.";
        }
        if (group.getGroupStatus() == GroupStatus.ACTIVE) {
            return "Group is already active.";
        }
        if (group.getGroupStatus() == GroupStatus.CLOSED) {
            return "Group is closed and cannot be activated. Contact Support for more information.";
        }

        if (group.getGroupMembers().size() < 3) {
            return "At least 3 members are required to activate the group.";
        }

        if (group.getGroupMembers().size() < group.getMemberCount()) {
            group.setMemberCount(group.getGroupMembers().size());
        }

        if (group.isGuaranteeRequired()) {
            for (GroupMember member : group.getGroupMembers()) {
                if (member.getGuaranteeBalance().compareTo(
                        group.getAmountToSave().multiply(BigDecimal.valueOf(group.getGroupMembers().size()))) == 0) {
                    return "All members must have a guaranteed balance equal to number of members to activate the group.";
                }
            }
        }
        group.setGroupStatus(GroupStatus.ACTIVE);
        group.setCurrentPayoutIndex(1);
        group.setNextContributionDate(LocalDate.now());
        groupRepository.save(group);

       boolean cycleProcessed = groupContributionService.processNextCycle(group.getId());

     // This or instead, return an object, with boolean and the updated next contribution date.
      Group updatedGroup = groupRepository.findById(group.getId()).orElseThrow(() -> new ApplicationException("Group not found"));
      
       if(cycleProcessed) {
        List<GroupContributionRecord> records = new ArrayList<>();

        for (GroupMember member : group.getGroupMembers()) {
            if(member.getPayoutIndex() != 2) {
                GroupContributionRecord record = GroupContributionRecord.builder()
                        .groupId(group.getId())
                        .cycleNumber(group.getCurrentCycle())
                        .userId(member.getUserId())
                        .amount(group.getAmountToSave())
                        .contributionStatus(ContributionStatus.DUE)
                        .nextCycleDate(updatedGroup.getNextContributionDate())
                        .build();
                records.add(record);
            } 
        }
        groupContributionRecordRepository.saveAll(records);

         return "Group activated successfully.";
       }


       return "Failed to activate group.";
    }


    @Transactional
    @Override
    public String deleteGroup(String groupNameCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found with email: " + email));
        Optional<Group> groupOptional = groupRepository.findByGroupCodeOrName(groupNameCode, groupNameCode);
        if (groupOptional.isEmpty()) {
            return "Group not found";
        }
        Group group = groupOptional.get();
        if (!group.getCreatorId().equals(user.getId())) {
            return "Only the group creator can delete the group.";
        }
        if (group.getGroupStatus() == GroupStatus.ACTIVE) {
            return "Active groups cannot be deleted.";
        }
        if (group.getGroupStatus() == GroupStatus.CLOSED) {
            return "Group is Closed, Please contact support for more information.";
        }

        List<GroupMember> members = group.getGroupMembers();
        if(members != null && !members.isEmpty()) {
            for (GroupMember member : members) {
                User memberUser = userRepository.findById(member.getUserId())
                        .orElseThrow(() -> new ApplicationException("User not found with ID: " + member.getUserId()));
                if (member.getGuaranteeBalance().compareTo(BigDecimal.ZERO) > 0) {
                    memberUser.getUserWallet().setAvailableBalance(
                            memberUser.getUserWallet().getAvailableBalance().add(member.getGuaranteeBalance()));
                }
                if (group.getGroupWallet().getAvailableBalance().compareTo(group.getAmountToSave()) >= 0) {
                    group.getGroupWallet().setAvailableBalance(
                            group.getGroupWallet().getAvailableBalance().subtract(group.getAmountToSave()));
                    memberUser.getUserWallet().setAvailableBalance(
                            memberUser.getUserWallet().getAvailableBalance().add(group.getAmountToSave()));
                }
                userRepository.save(memberUser);
                group.removeMember(member);
                groupRepository.save(group);
            }
        }

        groupRepository.delete(group);

        return "Group deleted successfully.";
    }


 
    private String generateUniqueGroupCode(GroupSavingsType savingsType) {

        String prefix;

        switch (savingsType) {
            case DAILY:
                prefix = "DLY";
                break;

            case WEEKLY:
                prefix = "WKLY";
                break;

            case BI_WEEKLY:
                prefix = "BIW";
                break;

            default:
                prefix = "MTH";
        }

        String groupCode;

        do {
            groupCode = prefix +
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase();

        } while (groupRepository.existsByGroupCode(groupCode));

        return groupCode;
    }

}
