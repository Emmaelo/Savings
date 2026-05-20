package com.emmanuelandsamuel.savings_project.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "group_contributions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GroupContributionRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID groupId;

    @Column(nullable = false)
    private int cycleNumber;

    private UUID userId;

    private LocalDate nextCycleDate;

    @Enumerated(EnumType.STRING)
    private ContributionStatus contributionStatus;

    private BigDecimal amount;

    private LocalDate paymentMadeOn;

    public enum ContributionStatus {
        DUE,
        PAID
    }

}
