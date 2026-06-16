package com.emmanuelandsamuel.savings_project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.emmanuelandsamuel.savings_project.entities.GroupClosureRequest;

@Repository
public interface GroupClosureRequestRepository extends JpaRepository<GroupClosureRequest, Integer>{
   boolean existsByGroupCode(String groupCode);
   boolean existsByGroupCodeAndUserEmail(String groupCode, String userEmail);
   GroupClosureRequest findByGroupCodeAndUserEmail(String groupCode, String userEmail);
   void deleteAllByGroupCode(String groupCode);

}
