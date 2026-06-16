package com.emmanuelandsamuel.savings_project.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.GroupMember;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID>{
     @Query("""
    SELECT DISTINCT gm
    FROM GroupMember gm
    JOIN FETCH gm.group g
    LEFT JOIN FETCH g.groupMembers
    WHERE gm.userEmail = :email
    ORDER BY gm.joinedAt ASC
""")
    List<GroupMember> findUserGroups(String email);

}
