package com.emmanuelandsamuel.savings_project.repositories;

import com.emmanuelandsamuel.savings_project.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Modifying
    @Query(value = """
            INSERT INTO users (
                id,
                email,
                firstname,
                lastname,
                password,
                phone_number,
                is_account_locked,
                failed_login_attempts,
                role,
                created_at
            ) VALUES (
                :id,
                :email,
                :firstname,
                :lastname,
                :password,
                :phoneNumber,
                :isAccountLocked,
                :failedLoginAttempts,
                :role,
                now()
            )
            ON CONFLICT (email) DO NOTHING
            """, nativeQuery = true)
    int insertUserIgnoreConflict(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("firstname") String firstname,
            @Param("lastname") String lastname,
            @Param("password") String password,
            @Param("phoneNumber") String phoneNumber,
            @Param("isAccountLocked") boolean isAccountLocked,
            @Param("failedLoginAttempts") int failedLoginAttempts,
            @Param("role") String role
    );
}
