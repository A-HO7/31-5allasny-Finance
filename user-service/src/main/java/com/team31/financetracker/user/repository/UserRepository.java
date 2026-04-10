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


    @Query(value = """
            SELECT
                u.id,
                u.name,
                COUNT(t.id) AS total_transactions,
                COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) AS completed_transactions,
                COUNT(CASE WHEN t.status = 'VOIDED' THEN 1 END) AS voided_transactions,
                COALESCE(SUM(CASE WHEN t.type = 'INCOME' AND t.status = 'COMPLETED' THEN t.amount ELSE 0 END), 0) AS total_income,
                COALESCE(SUM(CASE WHEN t.type = 'EXPENSE' AND t.status = 'COMPLETED' THEN t.amount ELSE 0 END), 0) AS total_expenses
            FROM users u
            LEFT JOIN transactions t ON u.id = t.user_id
            WHERE u.id = :userId
            GROUP BY u.id, u.name
            """, nativeQuery = true)
    Object[] getUserTransactionSummary(@Param("userId") Long userId);    



}