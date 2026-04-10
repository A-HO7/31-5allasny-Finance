package com.team31.financetracker.user.repository;

import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users WHERE " +
            "(:name IS NULL OR LOWER(name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:email IS NULL OR LOWER(email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
            "(:role IS NULL OR role = CAST(:role AS VARCHAR))",
            nativeQuery = true)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role);

    @Query(value = "SELECT * FROM users WHERE preferences ->> :key = :value", nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);
}