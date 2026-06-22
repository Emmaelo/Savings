package com.emmanuelandsamuel.savings_project.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emmanuelandsamuel.savings_project.dtos.requests.AddMemberRequest;
import com.emmanuelandsamuel.savings_project.dtos.requests.GroupRequest;
import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;

import com.emmanuelandsamuel.savings_project.entities.GroupMember;
import com.emmanuelandsamuel.savings_project.entities.GroupWallet;
import com.emmanuelandsamuel.savings_project.entities.GroupWalletLedger;

import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.entities.UserWalletLedger;
import com.emmanuelandsamuel.savings_project.enumerations.GroupSavingsType;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.enumerations.WalletStatus;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.exceptions.WalletNotFoundException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupContributionRecordRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.GroupService;
import com.emmanuelandsamuel.savings_project.utilities.AppExtensions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupServiceImplementation implements GroupService {
    private final GroupRepository groupRepository;
    private final UserWalletRepository userWalletRepository;
    private final GroupWalletLedgerRepository groupWalletLedgerRepository;
    private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
    private final UserWalletLedgerRepository userWalletLedgerRepository;
    private static final BigDecimal FEE = BigDecimal.valueOf(500);
    private static final BigDecimal CREATION_FEE = BigDecimal.valueOf(1000);
    private final GroupContributionRecordRepository groupContributionRepository;

    @Transactional
    public String createGroup(GroupRequest groupRequest) {
        if (groupRequest.getAmountToSave().compareTo(BigDecimal.valueOf(2000)) < 0) {
            return "Amount must be at least 2000.";
        }
        if (groupRequest.getMemberCount() < 3) {
            return "Member count must be at least 3.";
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(email)
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        if (wallet.getWalletStatus() == WalletStatus.SUSPENDED) {
            return "User is Suspended. Please Contact Customer Care for resolution";
        }

        if (wallet.getAvailableBalance().compareTo(groupRequest.getAmountToSave().add(CREATION_FEE)) < 0) {
            return "Insufficient funds in your wallet to create this group. Please fund your wallet and try again.";
        }

        GroupSavingsType groupSavingsType = GroupSavingsType.valueOf(groupRequest.getGroupSavingsType().toUpperCase());

        String groupCode = generateUniqueGroupCode(groupSavingsType);
        Group group = Group.builder()
                .name(groupRequest.getName())
                .creatorEmail(email)
                .amountToSave(groupRequest.getAmountToSave())
                .groupSavingsType(groupSavingsType)
                .memberCount(groupRequest.getMemberCount())
                .currentMemberCount(1)
                .groupStatus(GroupStatus.INACTIVE)
                .guaranteeRequired(groupRequest.getGuaranteeRequired().equalsIgnoreCase("true") ? true : false)
                .groupCode(groupCode)
                .build();

        group.addMember(
                GroupMember.builder()
                        .payoutIndex(1)
                        .guaranteeBalance(BigDecimal.ZERO)
                        .groupCode(groupCode)
                        .userEmail(email)
                        .joinedAt(LocalDateTime.now())
                        .build());

        wallet.setAvailableBalance(
                wallet.getAvailableBalance().subtract(groupRequest.getAmountToSave().add(FEE)));
        userWalletRepository.save(wallet);

        group.assignGroupWallet(GroupWallet.builder().build());
        group.getGroupWallet().setAvailableBalance(
                group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));
        groupRepository.save(group);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(groupRequest.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .source("Deposit by " + email)
                .entryType(LedgerEntryType.CREDIT)
                .userEmail(email)
                .build();
        groupWalletLedgerRepository.save(ledgerEntry);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(groupRequest.getAmountToSave())
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .source("USER_CREATE_GROUP")
                        .email(email)
                        .fee(FEE)
                        .build());

        companyWalletLedgerRepository.save(
                CompanyWalletLedger.builder()
                        .amount(FEE)
                        .source("Group Created by " + email)
                        .entryType(LedgerEntryType.CREDIT)
                        .build());

        return "Group created successfully";
    }

    @Transactional
    public String userJoinGroup(String groupCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(email)
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        Optional<Group> groupOptional = groupRepository.findByGroupCodeForUpdate(groupCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with code: " + groupCode;
        }
        Group group = groupOptional.get();
        if (wallet.getWalletStatus() == WalletStatus.SUSPENDED) {
            return "User is Suspended. Please Contact Customer Care for resolution";
        }

        if (wallet.getAvailableBalance().compareTo(group.getAmountToSave().add(FEE)) < 0) {
            return "Insufficient funds in your wallet to join this group. Please fund your wallet and try again.";
        }

        if (group.getGroupMembers().stream().anyMatch(member -> member.getUserEmail().equals(email))) {
            return "You are already a member of this group.";
        }

        if (group.getGroupStatus() == GroupStatus.ACTIVE || group.getGroupStatus() == GroupStatus.SUSPENDED
                || group.getGroupStatus() == GroupStatus.CLOSED) {
            return "You cannot join this group at the moment.";
        }

        if (group.getGroupMembers().size() >= group.getMemberCount()) {
            return "Group is filled. You cannot join this group.";
        }

        wallet.setAvailableBalance(
                wallet.getAvailableBalance().subtract(group.getAmountToSave().add(FEE)));

        userWalletRepository.save(wallet);

        List<GroupMember> members = group.getGroupMembers();
        int nextPayoutIndex = members.stream().mapToInt(GroupMember::getPayoutIndex).max().orElse(0) + 1;

        group.addMember(
                GroupMember.builder()

                        .payoutIndex(nextPayoutIndex)
                        .guaranteeBalance(BigDecimal.ZERO)
                        .groupCode(group.getGroupCode())
                        .userEmail(email)
                        .joinedAt(LocalDateTime.now())
                        .build());
        group.getGroupWallet().setAvailableBalance(
                group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));
        group.setCurrentMemberCount(group.getCurrentMemberCount() + 1);

        groupRepository.save(group);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(group.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.CREDIT)
                .userEmail(email)
                .source("Deposit by " + email)
                .build();

        groupWalletLedgerRepository.save(ledgerEntry);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(group.getAmountToSave())
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .source("USER_JOINED_GROUP")
                        .email(email)
                        .fee(FEE)
                        .build());

        companyWalletLedgerRepository.save(
                CompanyWalletLedger.builder()
                        .amount(FEE)
                        .source("Group Joined by " + email)
                        .entryType(LedgerEntryType.CREDIT)
                        .build());

        return "Joined Group successfully";
    }

    @Transactional
    public String userLeaveGroup(String groupCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(email)
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        Optional<Group> groupOptional = groupRepository.findByGroupCodeForUpdate(groupCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with code: " + groupCode;
        }
        Group group = groupOptional.get();

        if (group.getGroupStatus() == GroupStatus.ACTIVE || group.getGroupStatus() == GroupStatus.SUSPENDED) {
            return "You cannot leave this group at the moment. Group is either Active or Suspended";
        }
        if (group.getCreatorEmail().equals(email)) {
            return "As the creator of the group, you cannot leave the group. You can only delete the group.";
        }

        GroupMember member = group.getGroupMembers().stream()
                .filter(m -> m.getUserEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new ApplicationException("You are not a member of this group."));

        if (member.getGuaranteeBalance().compareTo(BigDecimal.ZERO) > 0) {
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(member.getGuaranteeBalance()));
        }

        if (group.getGroupWallet().getAvailableBalance().compareTo(group.getAmountToSave()) >= 0) {
            group.getGroupWallet().setAvailableBalance(
                    group.getGroupWallet().getAvailableBalance().subtract(group.getAmountToSave()));
            wallet.setAvailableBalance(
                    wallet.getAvailableBalance().add(group.getAmountToSave()));
        }
        group.removeMember(member);
        group.setCurrentMemberCount(group.getCurrentMemberCount() - 1);

        List<GroupMember> members = group.getGroupMembers()
                .stream()
                .sorted(Comparator.comparing(GroupMember::getJoinedAt))
                .toList();

        int i = 1;
        for (GroupMember m : members) {
            m.setPayoutIndex(i++);
        }

        groupRepository.save(group);

        userWalletRepository.save(wallet);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(group.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.DEBIT)
                .source("Left group by " + email)
                .userEmail(email)
                .build();

        groupWalletLedgerRepository.save(ledgerEntry);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(group.getAmountToSave().add(member.getGuaranteeBalance()))
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .source("USER_LEFT_GROUP")
                        .email(email)
                        .fee(BigDecimal.ZERO)
                        .build());

        return "Left Group successfully";
    }

    @Transactional
    public String activateGroup(String groupCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Group> groupOptional = groupRepository.findByGroupCodeForUpdate(groupCode);
        if (groupOptional.isEmpty()) {
            return "Group not found with code: " + groupCode;
        }
        Group group = groupOptional.get();

        if (!group.getCreatorEmail().equals(email)) {
            return "Only the group creator can activate the group.";
        }
        if (group.getGroupStatus() == GroupStatus.ACTIVE) {
            return "Group is already active.";
        }
        if (group.getGroupStatus() == GroupStatus.SUSPENDED) {
            return "Group is suspended and cannot be activated. Contact Support for more information.";
        }
        if (group.getGroupStatus() == GroupStatus.CLOSED) {
            return "Group is closed. Creator should delete group";
        }

        if (group.getGroupMembers().size() < 3) {
            return "At least 3 members are required to activate the group.";
        }

        if (group.isGuaranteeRequired()) {
            for (GroupMember member : group.getGroupMembers()) {
                if (member.getGuaranteeBalance().compareTo(
                        group.getAmountToSave().multiply(BigDecimal.valueOf(group.getGroupMembers().size() - 1))) < 0) {
                    return "All members must have a sufficient guaranteed balance to activate the group.";
                }
            }
        }

        if (group.getGroupMembers().size() < group.getMemberCount()) {
            group.setMemberCount(group.getGroupMembers().size());
        }

        group.setGroupStatus(GroupStatus.ACTIVE);
        group.setNextContributionDate(AppExtensions.calculateNextDate(LocalDate.now(), group.getGroupSavingsType()));

        GroupMember payoutMember = group.getGroupMembers().stream()
                .filter(m -> m.getPayoutIndex() == 1)
                .findFirst()
                .orElseThrow(() -> new ApplicationException("Payout member not found for current payout index"));

        boolean payoutSuccessful = payMember(group, payoutMember);

        if (!payoutSuccessful) {
            log.error("Failed to process payout for member: {}", payoutMember.getId());
            return "Failed to process Payout";
        }

        payoutMember.setHasReceivedCurrentCycle(true);

        group.setCurrentPayoutIndex(2);

        groupRepository.save(group);

        int nextPayoutIndex = group.getCurrentPayoutIndex();

        List<GroupContributionRecord> contributionRecords = group.getGroupMembers().stream()
                .filter(m -> m.getPayoutIndex() != nextPayoutIndex)
                .map(member -> {
                    GroupContributionRecord record = new GroupContributionRecord();

                    record.setGroupId(group.getId());
                    record.setCurrentIndex(nextPayoutIndex);
                    record.setGroupCode(group.getGroupCode());
                    record.setCycleNumber(group.getCurrentCycle());
                    record.setUserEmail(member.getUserEmail());
                    record.setAmount(group.getAmountToSave());
                    record.setNextContributionDate(group.getNextContributionDate());
                    record.setContributionStatus(GroupContributionRecord.ContributionStatus.DUE);

                    return record;
                })
                .toList();

        groupContributionRepository.saveAll(contributionRecords);

        return "Group Activated.";
    }

    @Transactional
    public String deleteGroup(String groupCode) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Group> groupOptional = groupRepository.findByGroupCodeForUpdate(groupCode);

        if (groupOptional.isEmpty()) {
            return "Group not found";
        }
        Group group = groupOptional.get();
        if (!group.getCreatorEmail().equals(email)) {
            return "Only the group creator can delete the group.";
        }
        if (group.getGroupStatus() == GroupStatus.ACTIVE) {
            return "Active groups cannot be deleted.";
        }
        if (group.getGroupStatus() == GroupStatus.SUSPENDED) {
            return "Group is Suspended, Please contact support for more information.";
        }

        BigDecimal balanceBefore = group.getGroupWallet().getAvailableBalance();

        List<GroupMember> members = new ArrayList<>(group.getGroupMembers());
        if (members != null && !members.isEmpty()) {
            for (GroupMember member : members) {

                UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(member.getUserEmail())
                        .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

                UserWalletLedger userWalletLedger = new UserWalletLedger();
                userWalletLedger.setWalletId(wallet.getId());
                userWalletLedger.setEntryType(LedgerEntryType.CREDIT);
                userWalletLedger.setSource("GROUP_DELETED");
                userWalletLedger.setEmail(member.getUserEmail());
                userWalletLedger.setFee(BigDecimal.ZERO);

                BigDecimal totalAmount = BigDecimal.ZERO;

                if (member.getGuaranteeBalance().compareTo(BigDecimal.ZERO) > 0) {
                    totalAmount = totalAmount.add(member.getGuaranteeBalance());
                    wallet.setAvailableBalance(
                            wallet.getAvailableBalance().add(member.getGuaranteeBalance()));
                }

                if (group.getGroupWallet().getAvailableBalance().compareTo(group.getAmountToSave()) >= 0) {

                    totalAmount = totalAmount.add(group.getAmountToSave());

                    group.getGroupWallet().setAvailableBalance(
                            group.getGroupWallet().getAvailableBalance().subtract(group.getAmountToSave()));

                    wallet.setAvailableBalance(
                            wallet.getAvailableBalance().add(group.getAmountToSave()));
                }

                userWalletLedger.setAmount(totalAmount);
                userWalletLedger.setBalanceAfter(wallet.getAvailableBalance());
                userWalletLedgerRepository.save(userWalletLedger);
                userWalletRepository.save(wallet);
            }
        }
        groupWalletLedgerRepository.save(GroupWalletLedger.builder()
                .amount(balanceBefore)
                .entryType(LedgerEntryType.DEBIT)
                .balanceAfter(BigDecimal.ZERO)
                .walletId(group.getGroupWallet().getId())
                .source("Group with groupCode " + groupCode + " deleted")
                .userEmail(group.getCreatorEmail())
                .build());

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

    private boolean payMember(Group group, GroupMember payoutMember) {

        BigDecimal payoutAmount = group.getGroupWallet().getAvailableBalance().subtract(FEE);

        group.getGroupWallet().setAvailableBalance(BigDecimal.ZERO);

        UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(payoutMember.getUserEmail())
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

        wallet.setAvailableBalance(wallet.getAvailableBalance().add(payoutAmount));

        userWalletRepository.save(wallet);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(payoutAmount)
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .fee(FEE)
                        .email(payoutMember.getUserEmail())
                        .source("GROUP_PAYOUT")
                        .build());

        groupWalletLedgerRepository.save(
                GroupWalletLedger.builder()
                        .walletId(group.getGroupWallet().getId())
                        .amount(payoutAmount)
                        .balanceAfter(group.getGroupWallet().getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .source("Group Payout to " + payoutMember.getUserEmail())
                        .userEmail(payoutMember.getUserEmail())
                        .build());

        companyWalletLedgerRepository.save(
                CompanyWalletLedger.builder()
                        .amount(FEE)
                        .source("Group Payout to" + payoutMember.getUserEmail())
                        .entryType(LedgerEntryType.CREDIT)
                        .build());
        return true;
    }



    @Transactional
    public String addMemberToGroup(AddMemberRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Group group = groupRepository.findByGroupCodeForUpdate(request.getGroupCode())
                .orElseThrow(() -> new ApplicationException("Group not found"));

        group.getGroupMembers().stream()
                .filter(m -> m.getUserEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new ApplicationException("You cannot add user to a group you belonged not to !!"));

        UserWallet wallet = userWalletRepository.findByEmailOrPhoneForUpdate(request.getEmailOrNumber())
                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));


         if (wallet.getAvailableBalance().compareTo(group.getAmountToSave().add(FEE)) < 0) {
            return request.getEmailOrNumber() + " no sufficient balance to join. Please fund wallet and try again.";
        }

        if (group.getGroupMembers().stream().anyMatch(member -> member.getUserEmail().equals(email))) {
            return request.getEmailOrNumber() + " is already a member of this group.";
        }

        if (group.getGroupStatus() == GroupStatus.ACTIVE || group.getGroupStatus() == GroupStatus.SUSPENDED
                || group.getGroupStatus() == GroupStatus.CLOSED) {
            return "You cannot add to this group at the moment.";
        }

        if (group.getGroupMembers().size() >= group.getMemberCount()) {
            return "Group is filled. User cannot be added this group.";
        }

        wallet.setAvailableBalance(
                wallet.getAvailableBalance().subtract(group.getAmountToSave().add(FEE)));

        userWalletRepository.save(wallet);

        List<GroupMember> members = group.getGroupMembers();
        int nextPayoutIndex = members.stream().mapToInt(GroupMember::getPayoutIndex).max().orElse(0) + 1;

        group.addMember(
                GroupMember.builder()

                        .payoutIndex(nextPayoutIndex)
                        .guaranteeBalance(BigDecimal.ZERO)
                        .groupCode(group.getGroupCode())
                        .userEmail(email)
                        .joinedAt(LocalDateTime.now())
                        .build());
        group.getGroupWallet().setAvailableBalance(
                group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));
        group.setCurrentMemberCount(group.getCurrentMemberCount() + 1);

        groupRepository.save(group);

        GroupWalletLedger ledgerEntry = GroupWalletLedger.builder()
                .walletId(group.getGroupWallet().getId())
                .amount(group.getAmountToSave())
                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                .entryType(LedgerEntryType.CREDIT)
                .userEmail(email)
                .source("Deposit by " + email)
                .build();

        groupWalletLedgerRepository.save(ledgerEntry);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(wallet.getId())
                        .amount(group.getAmountToSave())
                        .balanceAfter(wallet.getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .source("USER_JOINED_GROUP")
                        .email(email)
                        .fee(FEE)
                        .build());

        companyWalletLedgerRepository.save(
                CompanyWalletLedger.builder()
                        .amount(FEE)
                        .source("Group Joined by " + email)
                        .entryType(LedgerEntryType.CREDIT)
                        .build());

        return "User added successfully";

    }

}
