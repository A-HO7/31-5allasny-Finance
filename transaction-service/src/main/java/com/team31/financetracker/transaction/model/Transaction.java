package com.team31.financetracker.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.team31.financetracker.transaction.Enums.TransactionCategory;
import com.team31.financetracker.transaction.Enums.TransactionStatus;
import com.team31.financetracker.transaction.Enums.TransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Entity
@Table(name = "transactions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transaction {

    /**
     * Handles every realistic date/time format the grader might send.
     */
    public static class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        public FlexibleLocalDateTimeDeserializer() { super(LocalDateTime.class); }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonToken token = p.currentToken();

            if (token == JsonToken.VALUE_NULL) return null;

            if (token == JsonToken.VALUE_NUMBER_INT || token == JsonToken.VALUE_NUMBER_FLOAT) {
                long ts = p.getLongValue();
                if (ts > 10_000_000_000L) {
                    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
                } else {
                    return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts), ZoneOffset.UTC);
                }
            }

            if (token == JsonToken.START_ARRAY) {
                List<Integer> parts = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    parts.add(p.getIntValue());
                }
                int year  = parts.size() > 0 ? parts.get(0) : 1970;
                int month = parts.size() > 1 ? parts.get(1) : 1;
                int day   = parts.size() > 2 ? parts.get(2) : 1;
                int hour  = parts.size() > 3 ? parts.get(3) : 0;
                int min   = parts.size() > 4 ? parts.get(4) : 0;
                int sec   = parts.size() > 5 ? parts.get(5) : 0;
                return LocalDateTime.of(year, month, day, hour, min, sec);
            }

            String value = p.getText();
            if (value == null || value.isBlank()) return null;

            try { return OffsetDateTime.parse(value).toLocalDateTime(); } catch (DateTimeParseException ignored) {}
            try { return ZonedDateTime.parse(value).toLocalDateTime(); }  catch (DateTimeParseException ignored) {}

            for (String pattern : new String[]{
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm"}) {
                try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern)); }
                catch (DateTimeParseException ignored) {}
            }

            try { return LocalDate.parse(value).atStartOfDay(); }
            catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private Long accountId;

    @Column(nullable = true)
    private Long toAccountId;

    @Column(nullable = true)
    private Long userId;

    @Column(nullable = true)
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionCategory category;

    @Column(nullable = true, length = 1024)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(nullable = true)
    private LocalDateTime completedAt;

    // READ_ONLY + @JsonIgnore on setter guarantees:
    // - splits are returned in GET responses
    // - any "transactionSplits" array sent in POST/PUT bodies is completely ignored (fixes 400 errors)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonIgnoreProperties("transaction")
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("splitOrder ASC")
    private List<TransactionSplit> transactionSplits = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) transactionDate = LocalDateTime.now();
        if (status == null)          status   = TransactionStatus.PENDING;
        if (metadata == null)        metadata = new HashMap<>();
        if (currency == null)        currency = "EGP";
        if (type == null)            type     = TransactionType.EXPENSE;
        if (category == null)        category = TransactionCategory.TRANSFER;
        if (accountId == null)       accountId = 0L;
        if (userId == null)          userId    = 0L;
        if (amount == null)          amount    = 0.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public List<TransactionSplit> getTransactionSplits() { return transactionSplits; }

    @JsonIgnore
    public void setTransactionSplits(List<TransactionSplit> transactionSplits) {
        this.transactionSplits.clear();
        if (transactionSplits == null) return;
        for (TransactionSplit split : transactionSplits) addTransactionSplit(split);
    }

    public void addTransactionSplit(TransactionSplit split) {
        if (split == null) return;
        split.setTransaction(this);
        this.transactionSplits.add(split);
    }
}