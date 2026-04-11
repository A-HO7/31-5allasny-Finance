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


    public static class SplitDTO {

        private Long id;
        private Integer splitOrder;
        private String recipientName;
        private Double amount;
        private String description;
        private String status;
        private Map<String, Object> metadata;

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