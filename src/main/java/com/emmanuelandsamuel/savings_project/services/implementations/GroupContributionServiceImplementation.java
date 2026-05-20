package com.emmanuelandsamuel.savings_project.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;
import com.emmanuelandsamuel.savings_project.entities.GroupMember;
import com.emmanuelandsamuel.savings_project.entities.GroupWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.User;
import com.emmanuelandsamuel.savings_project.entities.UserWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord.ContributionStatus;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupContributionRecordRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.GroupContributionService;
import com.emmanuelandsamuel.savings_project.utilities.AppExtensions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupContributionServiceImplementation implements GroupContributionService {

    private final GroupRepository groupRepository;
    private final GroupContributionRecordRepository groupContributionRepository;
    private static final BigDecimal FEE = BigDecimal.valueOf(500);
    private static final UUID COMPANY_WALLET_ID = UUID.fromString("0820-0000-01");
    private final UserRepository userRepository;
    private final GroupWalletLedgerRepository groupWalletLedgerRepository;
    private final CompanyWalletRepository companyWalletRepository;
    private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
    private final UserWalletLedgerRepository userWalletLedgerRepository;

    @Transactional
    @Override
    public String payContribution(UUID groupId) {
        // Implementation for recording a member's contribution payment
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ApplicationException("Group not found"));

        group.getGroupMembers().stream()
                .filter(member -> member.getUserId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ApplicationException("You are not a member of this group"));

        GroupContributionRecord contribution = groupContributionRepository.findByGroupIdAndCycleNumberAndUserId(groupId,
                group.getCurrentCycle(), user.getId());

        if (contribution.getContributionStatus().equals(ContributionStatus.PAID)) {
            return "Payment already made for this cycle";
        }

        if (user.getUserWallet().getAvailableBalance().compareTo(group.getAmountToSave()) < 0) {
            return "Insufficient Wallet Balance.. Please fund your wallet";
        }

        user.getUserWallet()
                .setAvailableBalance(user.getUserWallet().getAvailableBalance().subtract(group.getAmountToSave()));

        group.getGroupWallet()
                .setAvailableBalance(group.getGroupWallet().getAvailableBalance().add(group.getAmountToSave()));

        userRepository.save(user);
        groupRepository.save(group);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(user.getUserWallet().getId())
                        .amount(group.getAmountToSave())
                        .balanceAfter(user.getUserWallet().getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .fee(BigDecimal.ZERO)
                        .bank(null)
                        .transactionReference(null)
                        .build());

        groupWalletLedgerRepository.save(
                GroupWalletLedger.builder()
                        .walletId(group.getGroupWallet().getId())
                        .amount(group.getAmountToSave())
                        .balanceAfter(group.getGroupWallet().getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .userId(user.getId())
                        .build());

        contribution.setContributionStatus(GroupContributionRecord.ContributionStatus.PAID);
        contribution.setPaymentMadeOn(LocalDate.now());
        groupContributionRepository.save(contribution);

        return "Contribution recorded successfully.";
    }

    @Transactional
    @Override
    public boolean processNextCycle(UUID groupId) {

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ApplicationException("Group not found"));

        List<GroupContributionRecord> records = groupContributionRepository.findByGroupId(groupId);

        if (!allRequiredMembersPaid(group, records)) {

            log.error("Not all required members have paid for the current cycle: {}", group.getId());

            return false;
        }

        List<GroupMember> members = group.getGroupMembers();

        int currentIndex = group.getCurrentPayoutIndex();

        GroupMember payoutMember = members.stream()
                .filter(m -> m.getPayoutIndex() == currentIndex)
                .findFirst()
                .orElseThrow();

        // Trigger payout

        boolean payoutSuccessful = payMember(group, payoutMember);

        if (!payoutSuccessful) {
            log.error("Failed to process payout for member: {}", payoutMember.getId());
            return false;
        }

        // Mark member as received
        payoutMember.setHasReceivedCurrentCycle(true);

        // Move to next index
        int nextIndex = currentIndex + 1;

        // If last member completed, restart
        if (nextIndex > members.size()) {

            nextIndex = 1;

            // Reset round tracking
            members.forEach(m -> m.setHasReceivedCurrentCycle(false));
            group.setCurrentCycle(group.getCurrentCycle() + 1);
        }

        group.setCurrentPayoutIndex(nextIndex);

        // Set next contribution date
        group.setNextContributionDate(
                AppExtensions.calculateNextDate(group.getNextContributionDate(), group.getGroupSavingsType()));
        groupRepository.save(group);

        return true;
    }

    private boolean payMember(Group group, GroupMember payoutMember) {

        BigDecimal payoutAmount = group.getGroupWallet().getAvailableBalance().subtract(FEE);

        group.getGroupWallet().setAvailableBalance(BigDecimal.ZERO);

        User user = userRepository.findById(payoutMember.getUserId())
                .orElseThrow(() -> new ApplicationException("User not found"));

        user.getUserWallet().setAvailableBalance(user.getUserWallet().getAvailableBalance().add(payoutAmount));

        userRepository.save(user);

        userWalletLedgerRepository.save(
                UserWalletLedger.builder()
                        .walletId(user.getUserWallet().getId())
                        .amount(payoutAmount)
                        .balanceAfter(user.getUserWallet().getAvailableBalance())
                        .entryType(LedgerEntryType.CREDIT)
                        .fee(FEE)
                        .bank(null)
                        .transactionReference(null)
                        .build());

        groupWalletLedgerRepository.save(
                GroupWalletLedger.builder()
                        .walletId(group.getGroupWallet().getId())
                        .amount(payoutAmount)
                        .balanceAfter(group.getGroupWallet().getAvailableBalance())
                        .entryType(LedgerEntryType.DEBIT)
                        .userId(payoutMember.getUserId())
                        .build());

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

        return true;
    }

    private boolean allRequiredMembersPaid(Group group, List<GroupContributionRecord> records) {

        if (group.getCurrentCycle() == 1) {

            return true;
        }

        if (records.stream()
                .filter(r -> r.getContributionStatus().equals(GroupContributionRecord.ContributionStatus.PAID))
                .count() == group.getMemberCount() - 1) {
            return true;
        }

        return false;
    }

}
