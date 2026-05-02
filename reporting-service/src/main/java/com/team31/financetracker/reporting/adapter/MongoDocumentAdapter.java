package com.team31.financetracker.reporting.adapter;

import com.team31.financetracker.reporting.dto.ReportAuditEventDTO;
import com.team31.financetracker.reporting.mongo.ReportAuditEvent;
import org.springframework.stereotype.Component;

/**
 * Adapter Pattern (DP-8) — S5 MongoDocumentAdapter.
 *
 * Converts a raw MongoDB document (ReportAuditEvent) into the service-layer
 * ReportAuditEventDTO. Following the pattern requirement, each adapter has a
 * single adapt(source) → targetDto method. No universal base type is used.
 */
@Component
public class MongoDocumentAdapter {

    /**
     * Adapts a raw MongoDB ReportAuditEvent document to a ReportAuditEventDTO.
     *
     * @param source The raw MongoDB document
     * @return The adapted DTO
     */
    public ReportAuditEventDTO adapt(ReportAuditEvent source) {
        if (source == null) return null;

        return new ReportAuditEventDTO(
                source.getId(),
                source.getReportId(),
                source.getAction(),
                source.getTimestamp(),
                source.getReportType(),
                source.getPagesGenerated(),
                source.getDetails()
        );
    }
}
