package com.team31.financetracker.reporting.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data MongoDB repository for report audit trail events.
 */
@Repository
public interface ReportAuditEventRepository extends MongoRepository<ReportAuditEvent, String> {

    /** Find all events for a specific report, ordered newest-first */
    List<ReportAuditEvent> findByReportIdOrderByTimestampDesc(Long reportId);

    /** Find events in a timestamp range (used by S5-F11 audit endpoint) */
    List<ReportAuditEvent> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /** Count events by action for a given report */
    long countByReportIdAndAction(Long reportId, String action);
}
