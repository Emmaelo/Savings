package com.emmanuelandsamuel.savings_project.entities;

import com.emmanuelandsamuel.savings_project.enumerations.GroupSavingsType;
import com.emmanuelandsamuel.savings_project.enumerations.GroupStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "groups",
        indexes = {
                @Index(name = "idx_group_name", columnList = "name"),
                @Index(name = "idx_group_code", columnList = "groupCode")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private UUID creatorId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountToSave;

    @Column(nullable = false, unique = true, updatable = false)
    private String groupCode;

    @Column(nullable = false)
    private int memberCount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupStatus groupStatus;

    @Column(nullable = false)
    private boolean guaranteeRequired;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupSavingsType groupSavingsType;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GroupMember> groupMembers = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_wallet_id", nullable = false, updatable = false)
    private GroupWallet groupWallet;

    @Column(nullable = false)
    @Builder.Default
    private int currentPayoutIndex = 1;

    @Column(nullable = false)
    @Builder.Default
    private int currentCycle = 1;

    private LocalDate nextContributionDate;

    public void addMember(GroupMember groupMember) {

        this.groupMembers.add(groupMember);

        groupMember.setGroup(this);
    }

    public void removeMember(GroupMember groupMember) {

        this.groupMembers.remove(groupMember);

        groupMember.setGroup(null);
    }

    public void assignGroupWallet(GroupWallet groupWallet) {
        this.groupWallet = groupWallet;
        if (groupWallet != null) {
            groupWallet.setGroup(this);
        }
    }


    
}
