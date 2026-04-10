package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByUserId(Long userId);
    List<Account> findByType(AccountType type);
    List<Account> findByStatus(AccountStatus status);

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE accounts
    SET status = :status
    WHERE id = :id
    """, nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    @Query(value = """
        SELECT a.* 
        FROM accounts a
        JOIN users u ON u.id = a.user_id
        WHERE u.email = :email
    """, nativeQuery = true)
    List<Account> findByUserEmail(@Param("email") String email);
}
