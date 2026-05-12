package com.team31.financetracker.account.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private String currency ="EGP";

    @Column(nullable = false)
    private Double balance=0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus  status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private Double rating =0.0;

    @Column(nullable = false)
    private Integer totalRatings =0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> accountDetails;

    @Column(nullable = false, updatable = false)
    @org.hibernate.annotations.CreationTimestamp // Required by TC_S2_08
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AccountStatement> accountStatements;

    @PrePersist
    void onCreate(){
        if (createdAt == null){
            createdAt = LocalDateTime.now();
        }
        if (totalRatings == null){
            totalRatings = 0;
        }
        if (rating == null){
            rating = 0.0;
        }
        if (balance == null){
            balance = Double.valueOf(0);
        }
        if (currency == null){
            currency = "EGP";
        }
        if (status == null){
            status=AccountStatus.ACTIVE;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public void setTotalRatings(Integer totalRatings) {
        this.totalRatings = totalRatings;
    }

    public Map<String, Object> getAccountDetails() {
        return accountDetails;
    }

    public void setAccountDetails(Map<String, Object> accountDetails) {
        this.accountDetails = accountDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<AccountStatement> getAccountStatements() {
        return accountStatements;
    }

    public void setAccountStatements(List<AccountStatement> accountStatements) {
        this.accountStatements = accountStatements;
    }
}
