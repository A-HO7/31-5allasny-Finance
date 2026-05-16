package com.team31.financetracker.user.repository;

import com.team31.financetracker.user.model.Role;
import com.team31.financetracker.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users u WHERE " +
            "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CAST(:name AS text), '%'))) AND " +
            "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:email AS text), '%'))) AND " +
            "(:role IS NULL OR CAST(u.role AS text) = CAST(:role AS text))",
            nativeQuery = true)
    List<User> searchUsers(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role);

    @Query(value = "SELECT * FROM users WHERE preferences ->> :key = :value", nativeQuery = true)
    List<User> findByPreference(@Param("key") String key, @Param("value") String value);

    @Query(value = """
            SELECT * FROM users u
            WHERE u.preferences IS NOT NULL
              AND u.preferences->>'defaultCurrency' = :currency
            """, nativeQuery = true)
    List<User> findUsersWithDefaultCurrency(@Param("currency") String currency);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    Optional<User> findByEmail(String email);
}
