package com.emmanuelandsamuel.savings_project.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.UserBankAccount;

@Repository
@SuppressWarnings("NullableProblems")
public interface UserBankAccountRepositories extends JpaRepository<UserBankAccount, Long>{
    List<UserBankAccount>findByUserEmail(String email);
    boolean existsByUserEmailAndAccountNumberAndBankCode(String email, String bankName, String bankCode);

}
