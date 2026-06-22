package com.emmanuelandsamuel.savings_project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.CompanyWallet;

@Repository
public interface CompanyWalletRepository extends JpaRepository<CompanyWallet, Integer>{

}
