package com.team31.financetracker.transaction.listener;

import com.team31.financetracker.contracts.events.ReportCompletedEvent;
import com.team31.financetracker.contracts.events.ReportFailedEvent;
import com.team31.financetracker.contracts.events.ReportInitiatedEvent;
import com.team31.financetracker.contracts.events.ReportRevertedEvent;
import com.team31.financetracker.contracts.events.TransactionVoidedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import com.team31.financetracker.transaction.model.Transaction;
import com.team31.financetracker.transaction.repository.TransactionRepository;
import com.team31.financetracker.transaction.service.EventPublisher;
import com.team31.financetracker.transaction.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionSagaFeedbackListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionSagaFeedbackListener.class);

    private final TransactionRepository transactionRepository;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TransactionSagaFeedbackListener(TransactionRepository transactionRepository,
            EventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "transaction.saga-feedback")
    @Transactional
    public void onSagaEvent(Message message,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            byte[] body = message.getBody();
            switch (routingKey) {
                case "report.initiated": {
                    ReportInitiatedEvent event = parsePayload(body, ReportInitiatedEvent.class);
                    log.info("Received report.initiated txnId={}", event.transactionId());
                    int rows = transactionRepository.markReportPendingIfCompleting(event.transactionId());
                    if (rows == 0)
                        log.warn("report.initiated: txnId={} not in COMPLETING — skipping", event.transactionId());
                    break;
                }
                case "report.completed": {
                    ReportCompletedEvent event = parsePayload(body, ReportCompletedEvent.class);
                    log.info("Received report.completed txnId={}", event.transactionId());
                    int rows = transactionRepository.markReportedIfReportPending(event.transactionId());
                    if (rows == 0)
                        log.warn("report.completed: txnId={} not in REPORT_PENDING — skipping", event.transactionId());
                    break;
                }
                case "report.failed": {
                    ReportFailedEvent event = parsePayload(body, ReportFailedEvent.class);
                    log.info("Received report.failed txnId={} reason={}", event.transactionId(), event.reason());

                    Transaction tx = transactionRepository.findById(event.transactionId()).orElse(null);
                    String previousStatus = (tx != null && tx.getStatus() != null) ? tx.getStatus().name() : null;

                    int rows = transactionRepository.markReportFailedIfReportPending(event.transactionId());
                    if (rows == 0) {
                        log.warn("report.failed: txnId={} not in REPORT_PENDING — skipping compensation",
                                event.transactionId());
                        break;
                    }

                    // publish compensation void
                    eventPublisher.publish("transaction.events", "transaction.voided",
                            new TransactionVoidedEvent(
                                    event.transactionId(),
                                    event.userId(),
                                    tx != null ? tx.getAccountId() : null,
                                    tx != null ? tx.getToAccountId() : null,
                                    tx != null && tx.getCategory() != null ? tx.getCategory().name() : null,
                                    tx != null && tx.getType() != null ? tx.getType().name() : null,
                                    tx != null ? tx.getAmount() : null,
                                    previousStatus,
                                    event.reason()));
                    break;
                }
                case "report.reverted": {
                    ReportRevertedEvent event = parsePayload(body, ReportRevertedEvent.class);
                    log.info("Received report.reverted txnId={}", event.transactionId());
                    int rows = transactionRepository.markRevertedIfReportFailed(event.transactionId());
                    if (rows == 0)
                        log.warn("report.reverted: txnId={} not in REPORT_FAILED — skipping", event.transactionId());
                    break;
                }
                default:
                    log.warn("Unhandled routing key {} on transaction.saga-feedback", routingKey);
            }
        } catch (Exception ex) {
            log.error("Failed to handle saga-feedback message rk={}", routingKey, ex);
            throw ex; // let listener framework route to DLQ
        }
    }

    private <T> T parsePayload(byte[] body, Class<T> type) {
        if (body == null || body.length == 0) {
            return null;
        }
        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8).trim();
        try {
            return objectMapper.readValue(text, type);
        } catch (Exception ex) {
            try {
                String cleaned = text.replace("\\\"", "\"").replace("\\\\", "\\");
                return objectMapper.readValue(cleaned, type);
            } catch (Exception inner) {
                throw new IllegalArgumentException("Failed to parse saga payload for " + type.getSimpleName(), inner);
            }
        }
    }
}
