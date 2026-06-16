package com.emmanuelandsamuel.savings_project.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GroupMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

 
    @Column(nullable = false)
    private String groupCode;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = true)
    private int payoutIndex; // Position in rotation queue (1, 2, 3, ...)

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal guaranteeBalance = BigDecimal.ZERO; 

    @Column(nullable = false)
    @Builder.Default
    private int missedContributions = 0; // Count of missed payments  have not implemented this anywhere...

    @Column(nullable = false)
    @Builder.Default
    private boolean hasReceivedCurrentCycle = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean paidCurrentCycle = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private LocalDateTime joinedAt;
}
