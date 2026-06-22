package com.emmanuelandsamuel.savings_project.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.UserWallet;

import jakarta.persistence.LockModeType;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {

@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM UserWallet w WHERE w.user.email = :email")
Optional<UserWallet> findByUserEmailForUpdate(String email);


@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT w 
    FROM UserWallet w 
    WHERE w.user.email = :identifier 
       OR w.user.phoneNumber = :identifier
    """)
Optional<UserWallet> findByEmailOrPhoneForUpdate(String identifier);


Optional<UserWallet> findByUserEmail(String email);

}
