package com.team31.financetracker.user.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    public enum Status {
        PENDING,
        SENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String routingKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public OutboxEvent() {
    }

    public OutboxEvent(Long id, String routingKey, String payload, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public OutboxEvent(String routingKey, String payload, Status status, LocalDateTime createdAt) {
        this.routingKey = routingKey;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
