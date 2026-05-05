package com.team31.financetracker.reporting.adapter;

import com.team31.financetracker.reporting.dto.TemplateUsageDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adapter Pattern (DP-7/DP-8) — S5 ObjectArrayDtoAdapter for TemplateUsageDTO (S5-F9).
 *
 * S5-F9 (getTopUsedTemplates) uses a native SQL query returning Object[] rows.
 * This adapter encapsulates the raw-to-DTO mapping, removing it from the service.
 */
@Component
public class TemplateUsageAdapter implements ObjectArrayDtoAdapter<TemplateUsageDTO> {

    /**
     * Adapts a single Object[] row from the findTopUsedTemplates native query to TemplateUsageDTO.
     *
     * Row layout (matches SavedReportRepository.findTopUsedTemplates):
     *   [0] templateId  (Long)
     *   [1] code        (String)
     *   [2] templateType(String)
     *   [3] maxPages    (Double)
     *   [4] timesUsed   (Integer)
     *   [5] totalPagesGenerated (Double)
     *   [6] active      (Boolean)
     *   [7] expiryDate  (Timestamp | LocalDateTime | String | null)
     */
    @Override
    public TemplateUsageDTO adapt(Object[] row) {
        if (row == null) return null;

        Long templateId        = ((Number) row[0]).longValue();
        String code            = (String) row[1];
        String templateType    = (String) row[2];
        Double maxPages        = ((Number) row[3]).doubleValue();
        Integer timesUsed      = ((Number) row[4]).intValue();
        Double totalPagesGen   = ((Number) row[5]).doubleValue();
        Boolean active         = (Boolean) row[6];

        LocalDateTime expiryDate = null;
        if (row[7] != null) {
            if (row[7] instanceof java.sql.Timestamp ts) {
                expiryDate = ts.toLocalDateTime();
            } else if (row[7] instanceof LocalDateTime ldt) {
                expiryDate = ldt;
            } else {
                expiryDate = LocalDateTime.parse(row[7].toString());
            }
        }
        Boolean expired = expiryDate != null && expiryDate.isBefore(LocalDateTime.now());

        return TemplateUsageDTO.builder()
                .templateId(templateId)
                .code(code)
                .templateType(templateType)
                .maxPages(maxPages)
                .timesUsed(timesUsed)
                .totalPagesGenerated(totalPagesGen)
                .active(active)
                .expired(expired)
                .build();
    }
}
