package com.emmanuelandsamuel.savings_project.services.implementations;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.emmanuelandsamuel.savings_project.entities.Group;
import com.emmanuelandsamuel.savings_project.entities.UserWallet;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;
import com.emmanuelandsamuel.savings_project.enumerations.WalletStatus;
import com.emmanuelandsamuel.savings_project.repositories.GroupRepository;
import com.emmanuelandsamuel.savings_project.repositories.UserWalletRepository;
import com.emmanuelandsamuel.savings_project.services.interfaces.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImplementation implements AdminService{
    private final UserWalletRepository walletRepository;
    private final GroupRepository groupRepository;

    public String suspendUserWallet(String userEmail, String message) {

        Optional<UserWallet> usr = walletRepository.findByUserEmail(userEmail);
        if (usr.isEmpty()) {
            return "user not found";
        }
        UserWallet wallet = usr.get();

        wallet.setWalletStatus(WalletStatus.SUSPENDED);
        log.info(userEmail + "wallet suspended for : " + message);;
        walletRepository.save(wallet);

        return "User's wallet Suspended";
    }

    public String suspendGroup(String groupCode, String message) {

        Optional<Group> grp = groupRepository.findByGroupCode(groupCode);
        if (grp.isEmpty()) {
            return "Group not found";
        }
        Group group = grp.get();
        group.setGroupStatus(GroupStatus.CLOSED);
        log.info("Group with group code "+groupCode + " suspended for : "+ message );
        groupRepository.save(group);

        return "Group Suspended";
    }


}
