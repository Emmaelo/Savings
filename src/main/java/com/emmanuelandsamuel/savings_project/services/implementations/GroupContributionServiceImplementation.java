package com.emmanuelandsamuel.savings_project.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emmanuelandsamuel.savings_project.dtos.requests.GuaranteeBalanceRequest;
import com.emmanuelandsamuel.savings_project.entities.CompanyWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord;
import com.emmanuelandsamuel.savings_project.entities.GroupMember;
import com.emmanuelandsamuel.savings_project.entities.GroupClosureRequest;
import com.emmanuelandsamuel.savings_project.entities.GroupWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.entities.UserWalletLedger;
import com.emmanuelandsamuel.savings_project.entities.GroupContributionRecord.ContributionStatus;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;
import com.emmanuelandsamuel.savings_project.enumerations.LedgerEntryType;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.exceptions.WalletNotFoundException;
import com.emmanuelandsamuel.savings_project.repositories.CompanyWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupContributionRecordRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupClosureRequestRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.GroupWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletLedgerRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
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
        private final UserWalletRepository userWalletRepository;
        private final GroupWalletLedgerRepository groupWalletLedgerRepository;
        private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
        private final UserWalletLedgerRepository userWalletLedgerRepository;
        private final GroupClosureRequestRepository groupClosureRequestRepository;

        @Transactional
        @Override
        public String payContribution(String groupCode) {

                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(email)
                                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

                Group group = groupRepository.findByGroupCodeForUpdate(groupCode)
                                .orElseThrow(() -> new ApplicationException("Group not found"));

                GroupMember groupMember = group.getGroupMembers().stream()
                                .filter(m -> m.getUserEmail().equals(wallet.getUserEmail()))
                                .findFirst()
                                .orElseThrow(() -> new ApplicationException("Group member not found"));

                Optional<GroupContributionRecord> optionalContribution = groupContributionRepository
                                .findByGroupIdAndCycleNumberAndUserEmailAndCurrentIndex(
                                                group.getId(),
                                                group.getCurrentCycle(),
                                                wallet.getUserEmail(),
                                                group.getCurrentPayoutIndex());

                if (optionalContribution.isEmpty()) {
                        return "You are not expected to contribute for this cycle. Please wait for the next cycle.";
                }

                if (optionalContribution.get().getContributionStatus() == ContributionStatus.PAID) {
                        return "You have already paid for this cycle.";
                }

                if (wallet.getAvailableBalance().compareTo(group.getAmountToSave()) < 0) {
                        return "Insufficient Wallet Balance.. Please fund your wallet";
                }

                wallet.setAvailableBalance(wallet.getAvailableBalance()
                                .subtract(group.getAmountToSave()));

                group.getGroupWallet()
                                .setAvailableBalance(group.getGroupWallet().getAvailableBalance()
                                                .add(group.getAmountToSave()));
                groupMember.setPaidCurrentCycle(true);

                userWalletRepository.save(wallet);
                groupRepository.save(group);

                userWalletLedgerRepository.save(
                                UserWalletLedger.builder()
                                                .walletId(wallet.getId())
                                                .amount(group.getAmountToSave())
                                                .balanceAfter(wallet.getAvailableBalance())
                                                .entryType(LedgerEntryType.DEBIT)
                                                .fee(BigDecimal.ZERO)
                                                .email(email)
                                                .source("GROUP_CONTRIBUTE_TO " + groupCode)
                                                .build());

                groupWalletLedgerRepository.save(
                                GroupWalletLedger.builder()
                                                .walletId(group.getGroupWallet().getId())
                                                .amount(group.getAmountToSave())
                                                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                                                .entryType(LedgerEntryType.CREDIT)
                                                .userEmail(email)
                                                .source("Group Contribution from " + email)
                                                .build());

                GroupContributionRecord groupContributionRecord = optionalContribution.get();
                groupContributionRecord.setContributionStatus(ContributionStatus.PAID);
                groupContributionRecord.setPaymentMadeOn(LocalDate.now());

                groupContributionRepository.save(groupContributionRecord);

                return "Contribution recorded successfully.";
        }

        @Transactional
        @Override
        public String processNextCycle(String groupCode) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                Group group = groupRepository.findByGroupCodeForUpdate(groupCode)
                                .orElseThrow(() -> new ApplicationException("Group not found"));

                if (!group.getCreatorEmail().equals(email)) {
                        return "Only the group creator can process Next Cycle.";
                }

                if (group.getGroupStatus() != GroupStatus.ACTIVE) {
                        return "Group is Suspended or Inactive.. Contact Customer Support";
                }

                if (group.getNextContributionDate().isAfter(LocalDate.now())) {
                        return "Payment date has not been reached yet. Contact admin if you think this is an error.";
                }

                List<GroupContributionRecord> records = groupContributionRepository
                                .findByGroupIdAndCycleNumberAndCurrentIndex(
                                                group.getId(),
                                                group.getCurrentCycle(),
                                                group.getCurrentPayoutIndex());

                if (!allRequiredMembersPaid(records)) {
                        return "Not all required members have paid for the current cycle.";
                }

                List<GroupMember> members = group.getGroupMembers();

                int currentIndex = group.getCurrentPayoutIndex();

                GroupMember payoutMember = members.stream()
                                .filter(m -> m.getPayoutIndex() == currentIndex)
                                .findFirst()
                                .orElseThrow();

                boolean payoutSuccessful = payMember(group, payoutMember);

                if (!payoutSuccessful) {
                        log.error("Failed to process payout for member: {}", payoutMember.getId());
                        return "Failed to process payout for member: " + payoutMember.getId();
                }

                payoutMember.setHasReceivedCurrentCycle(true);

                int nextIndex = currentIndex + 1;

                boolean groupClosureRequest = groupClosureRequestRepository.existsByGroupCode(groupCode);

                // If last member completed, restart
                if (nextIndex > members.size()) {
                        if (groupClosureRequest) {
                                deleteVotes(groupCode);
                                group.setGroupStatus(GroupStatus.CLOSED);
                                return "Group closed";
                        }

                        nextIndex = 1;

                        members.forEach(m -> m.setHasReceivedCurrentCycle(false));
                        group.setCurrentCycle(group.getCurrentCycle() + 1);

                }

                members.forEach(m -> m.setPaidCurrentCycle(false));
                group.setCurrentPayoutIndex(nextIndex);
                group.setNextContributionDate(
                                AppExtensions.calculateNextDate(LocalDate.now(), group.getGroupSavingsType()));

                createContributionRecords(group, nextIndex);

                groupRepository.save(group);

                return "Cycle processed successfully.";

        }

        private void createContributionRecords(Group group, int nextIndex) {

                List<GroupContributionRecord> records = group.getGroupMembers()
                                .stream()
                                .filter(member -> member.getPayoutIndex() != nextIndex)
                                .map(member -> {
                                        GroupContributionRecord record = new GroupContributionRecord();
                                        record.setGroupId(group.getId());
                                        record.setGroupCode(group.getGroupCode());
                                        record.setAmount(group.getAmountToSave());
                                        record.setUserEmail(member.getUserEmail());
                                        record.setContributionStatus(GroupContributionRecord.ContributionStatus.DUE);
                                        record.setCycleNumber(group.getCurrentCycle());
                                        record.setNextContributionDate(group.getNextContributionDate());
                                        record.setCurrentIndex(nextIndex);

                                        return record;
                                })
                                .toList();

                groupContributionRepository.saveAll(records);
        }

        private boolean allRequiredMembersPaid(List<GroupContributionRecord> records) {

                return records.stream()
                                .allMatch(r -> r.getContributionStatus() == GroupContributionRecord.ContributionStatus.PAID);
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
                                                .source("Group Payout to " + payoutMember.getUserEmail())
                                                .entryType(LedgerEntryType.CREDIT)
                                                .build());
                return true;
        }

        public String requestOrRemoveClosure(String groupCode) {

                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                Group group = groupRepository.findByGroupCode(groupCode)
                                .orElseThrow(() -> new ApplicationException("Group not found"));

                boolean member = group.getGroupMembers().stream()
                                .anyMatch(m -> m.getUserEmail().equals(email));
                if (!member) {
                        throw new ApplicationException("You are not a member of this group");
                }

                boolean groupVote = groupClosureRequestRepository.existsByGroupCodeAndUserEmail(groupCode, email);
                if (!groupVote) {
                        groupClosureRequestRepository.save(
                                        GroupClosureRequest.builder().userEmail(email).groupCode(groupCode).build());
                } else {
                        groupClosureRequestRepository.delete(
                                        groupClosureRequestRepository.findByGroupCodeAndUserEmail(groupCode, email));
                }

                return "Request Successfull";

        }

        private void deleteVotes(String groupCode) {
                groupClosureRequestRepository.deleteAllByGroupCode(groupCode);

        }

        @Transactional
        public String fundGuaranteeBalance(GuaranteeBalanceRequest request) {
                String email = SecurityContextHolder.getContext().getAuthentication().getName();

                UserWallet wallet = userWalletRepository.findByUserEmailForUpdate(email)
                                .orElseThrow(() -> new WalletNotFoundException("User wallet not found"));

                if (wallet.getAvailableBalance().compareTo(request.getAmount()) < 0) {
                        return "Insufficient Balance";
                }

                Group group = groupRepository.findByGroupCode(request.getGroupCode())
                                .orElseThrow(() -> new ApplicationException("Group not found"));

                GroupMember groupMember = group.getGroupMembers().stream()
                                .filter(m -> m.getUserEmail().equals(wallet.getUserEmail()))
                                .findFirst()
                                .orElseThrow(() -> new ApplicationException("Group member not found"));

                groupMember.setGuaranteeBalance(groupMember.getGuaranteeBalance().add(request.getAmount()));
                wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.getAmount()));

                groupRepository.save(group);
                userWalletRepository.save(wallet);
                userWalletLedgerRepository.save(UserWalletLedger.builder()
                                .walletId(wallet.getId())
                                .amount(request.getAmount())
                                .entryType(LedgerEntryType.DEBIT)
                                .email(email)
                                .source("GUARANTEE_BALANCE")
                                .fee(BigDecimal.ZERO)
                                .balanceAfter(wallet.getAvailableBalance())
                                .build());

                return "Guarantee balance funded";
        }


}
