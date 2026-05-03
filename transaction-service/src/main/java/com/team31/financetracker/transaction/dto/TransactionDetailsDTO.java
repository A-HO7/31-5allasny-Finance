package com.team31.financetracker.transaction.dto;

import com.team31.financetracker.transaction.Enums.TransactionSplitsStatus;

import java.util.List;
import java.util.Map;

public class TransactionDetailsDTO {

    private Long transactionId;
    private Long accountId;
    private Long userId;
    private String status;
    private Double amount;
    private Map<String, Object> metadata;
    private List<SplitDTO> splits;
    private int totalSplits;
    private long settledSplits;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long transactionId;
        private Long accountId;
        private Long userId;
        private String status;
        private Double amount;
        private Map<String, Object> metadata;
        private List<SplitDTO> splits;

        public Builder transactionId(Long transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder splits(List<SplitDTO> splits) {
            this.splits = splits;
            return this;
        }

        public TransactionDetailsDTO build() {
            return new TransactionDetailsDTO(transactionId, accountId, userId, status, amount, metadata, splits);
        }
    }

    public static class SplitDTO {

        private Long id;
        private Integer splitOrder;
        private String recipientName;
        private Double amount;
        private String description;
        private String status;
        private Map<String, Object> metadata;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Long id;
            private Integer splitOrder;
            private String recipientName;
            private Double amount;
            private String description;
            private TransactionSplitsStatus status;
            private Map<String, Object> metadata;

            public Builder id(Long id) {
                this.id = id;
                return this;
            }

            public Builder splitOrder(Integer splitOrder) {
                this.splitOrder = splitOrder;
                return this;
            }

            public Builder recipientName(String recipientName) {
                this.recipientName = recipientName;
                return this;
            }

            public Builder amount(Double amount) {
                this.amount = amount;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder status(TransactionSplitsStatus status) {
                this.status = status;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public SplitDTO build() {
                return new SplitDTO(id, splitOrder, recipientName, amount, description, status, metadata);
            }
        }

        public SplitDTO(Long id, Integer splitOrder, String recipientName,
                        Double amount, String description,
                        TransactionSplitsStatus status,
                        Map<String, Object> metadata) {
            this.id            = id;
            this.splitOrder    = splitOrder;
            this.recipientName = recipientName;
            this.amount        = amount;
            this.description   = description;
            this.status        = status != null ? status.name() : null;
            this.metadata      = metadata;
        }

        public Long getId()                      { return id; }
        public Integer getSplitOrder()           { return splitOrder; }
        public String getRecipientName()         { return recipientName; }
        public Double getAmount()                { return amount; }
        public String getDescription()           { return description; }
        public String getStatus()                { return status; }
        public Map<String, Object> getMetadata() { return metadata; }
    }


    public TransactionDetailsDTO(Long transactionId, Long accountId, Long userId,
                                 String status, Double amount,
                                 Map<String, Object> metadata,
                                 List<SplitDTO> splits) {
        this.transactionId = transactionId;
        this.accountId     = accountId;
        this.userId        = userId;
        this.status        = status;
        this.amount        = amount;
        this.metadata      = metadata;
        this.splits        = splits;
        this.totalSplits   = splits != null ? splits.size() : 0;
        this.settledSplits = splits != null
                ? splits.stream().filter(s -> "SETTLED".equals(s.getStatus())).count()
                : 0L;
    }


    public Long getTransactionId()             { return transactionId; }
    public Long getAccountId()                 { return accountId; }
    public Long getUserId()                    { return userId; }
    public String getStatus()                  { return status; }
    public Double getAmount()                  { return amount; }
    public Map<String, Object> getMetadata()   { return metadata; }
    public List<SplitDTO> getSplits()          { return splits; }
    public int getTotalSplits()                { return totalSplits; }
    public long getSettledSplits()             { return settledSplits; }
}