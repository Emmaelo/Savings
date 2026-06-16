package com.emmanuelandsamuel.savings_project.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.Transactions;

import jakarta.persistence.LockModeType;

@Repository
public interface TransactionRepository extends JpaRepository<Transactions, UUID> {

    @Query("SELECT t FROM Transactions t WHERE t.user.email = :userEmail")
    Optional<Transactions> findByUserEmail(String userEmail);

    
    Optional<Transactions> findByTransactionReference(String transactionReference);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query( "SELECT t FROM Transactions t WHERE t.payStackReference = :payStackReference" )
     Optional<Transactions> findByPayStackReferenceForUpdate(String payStackReference);

}
