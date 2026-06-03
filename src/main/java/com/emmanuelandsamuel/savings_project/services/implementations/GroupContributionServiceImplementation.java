package com.emmanuelandsamuel.savings_project.services.implementations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        private final UserRepository userRepository;
        private final GroupWalletLedgerRepository groupWalletLedgerRepository;
        private final CompanyWalletLedgerRepository companyWalletLedgerRepository;
        private final UserWalletLedgerRepository userWalletLedgerRepository;

        @Transactional
        @Override
        public String payContribution(String groupCode) {

                // String email =
                // SecurityContextHolder.getContext().getAuthentication().getName();
                String email = "emmanuelezeuchegbu@gmail.com";

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ApplicationException("User not found"));

                Group group = groupRepository.findByGroupCode(groupCode)
                                .orElseThrow(() -> new ApplicationException("Group not found"));

                group.getGroupMembers().stream()
                                .filter(m -> m.getUserId().equals(user.getId()))
                                .findFirst()
                                .orElseThrow(() -> new ApplicationException("Group member not found"));

                Optional<GroupContributionRecord> optionalContribution = groupContributionRepository
                                .findByGroupIdAndCycleNumberAndUserId(
                                                group.getId(),
                                                group.getCurrentCycle(),
                                                user.getId());

                if (optionalContribution.isEmpty()) {
                        return "You are not expected to contribute for this cycle. Please wait for the next cycle.";
                }

                if (optionalContribution.get().getContributionStatus() == ContributionStatus.PAID) {
                        return "You have already paid for this cycle.";
                }

                if (user.getUserWallet().getAvailableBalance().compareTo(group.getAmountToSave()) < 0) {
                        return "Insufficient Wallet Balance.. Please fund your wallet";
                }

                user.getUserWallet()
                                .setAvailableBalance(user.getUserWallet().getAvailableBalance()
                                                .subtract(group.getAmountToSave()));

                group.getGroupWallet()
                                .setAvailableBalance(group.getGroupWallet().getAvailableBalance()
                                                .add(group.getAmountToSave()));

                userRepository.save(user);
                groupRepository.save(group);

                userWalletLedgerRepository.save(
                                UserWalletLedger.builder()
                                                .walletId(user.getUserWallet().getId())
                                                .amount(group.getAmountToSave())
                                                .balanceAfter(user.getUserWallet().getAvailableBalance())
                                                .entryType(LedgerEntryType.DEBIT)
                                                .fee(BigDecimal.ZERO)
                                                .source("Group Contribution to " + groupCode)
                                                .build());

                groupWalletLedgerRepository.save(
                                GroupWalletLedger.builder()
                                                .walletId(group.getGroupWallet().getId())
                                                .amount(group.getAmountToSave())
                                                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                                                .entryType(LedgerEntryType.CREDIT)
                                                .userId(user.getId())
                                                .source("Group Contribution from " + user.getEmail())
                                                .build());

                GroupContributionRecord groupContributionRecord = optionalContribution.get();
                groupContributionRecord.setContributionStatus(ContributionStatus.PAID);
                groupContributionRecord.setPaymentMadeOn(LocalDate.now());

                groupContributionRepository.save(groupContributionRecord);

                return "Contribution recorded successfully.";
        }

        @Transactional
        @Override
        public String processNextCycle(UUID groupId) {

                Group group = groupRepository.findById(groupId)
                                .orElseThrow(() -> new ApplicationException("Group not found"));
                
        
                if(group.getNextContributionDate().isAfter(LocalDate.now())) {
                        return "Payment date has not been reached yet. Contact admin if you think this is an error.";
                }

                List<GroupContributionRecord> records = groupContributionRepository.findByGroupIdAndCycleNumber(groupId,
                                group.getCurrentCycle());


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

                // If last member completed, restart
                if (nextIndex > members.size()) {

                        nextIndex = 1;

                        
                        members.forEach(m -> m.setHasReceivedCurrentCycle(false));
                        group.setCurrentCycle(group.getCurrentCycle() + 1);
                        group.setCurrentPayoutIndex(nextIndex);
                }

                group.setCurrentPayoutIndex(nextIndex);

                // Set next contribution date
                group.setNextContributionDate(
                                AppExtensions.calculateNextDate(group.getNextContributionDate(),
                                                group.getGroupSavingsType()));
                groupRepository.save(group);


                // Set up contribution records for next cycle

                return "Cycle processed successfully.";
        }



        private boolean allRequiredMembersPaid(List<GroupContributionRecord> records) {

                return records.stream()
                                .allMatch(r -> r.getContributionStatus() == GroupContributionRecord.ContributionStatus.PAID);
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
                                                .source("Group Payout")
                                                .build());

                groupWalletLedgerRepository.save(
                                GroupWalletLedger.builder()
                                                .walletId(group.getGroupWallet().getId())
                                                .amount(payoutAmount)
                                                .balanceAfter(group.getGroupWallet().getAvailableBalance())
                                                .entryType(LedgerEntryType.DEBIT)
                                                .source("Group Payout to " + user.getEmail())
                                                .userId(payoutMember.getUserId())
                                                .build());

                companyWalletLedgerRepository.save(
                                CompanyWalletLedger.builder()
                                                .amount(FEE)
                                                .source("Group Payout to" + user.getEmail())
                                                .entryType(LedgerEntryType.CREDIT)
                                                .build());
                return true;
        }

}
