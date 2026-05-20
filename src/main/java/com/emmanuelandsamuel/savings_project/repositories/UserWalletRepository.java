package com.emmanuelandsamuel.savings_project.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.UserWallet;

@Repository
public interface UserWalletRepository extends JpaRepository<UserWallet, UUID> {

@Query("SELECT w FROM UserWallet w WHERE w.user.email = :email")
Optional<UserWallet> findByUserEmail(String email);
}
