package com.emmanuelandsamuel.savings_project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.Banks;

@Repository
public interface BankRepository extends JpaRepository<Banks, Long> {
    boolean existsByCode(String code);
}
