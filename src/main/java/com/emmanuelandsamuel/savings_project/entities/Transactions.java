package com.emmanuelandsamuel.savings_project.entities;

import java.math.BigDecimal;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Table(name = "transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class Transactions {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal requestAmount;

    @Column(nullable = true)
    private BigDecimal amountPaid;

    @Column(unique = true, nullable = true)
    private String payStackReference;

    @Column(nullable = true)
    private Long payStackTransactionId;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @Column(nullable = true)
    private String accessCode;

    @Column(nullable = false)
    private String userEmail;


    @Column(nullable = true)
    private String authorizationUrl;

    @Column(nullable = true)
    private String bank;

    @Column(nullable = true)
    private String paidAt;

    @Column(nullable = true)
    private String transactionType;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private UserEntity user;


}
