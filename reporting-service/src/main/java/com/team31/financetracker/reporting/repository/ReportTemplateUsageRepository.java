package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ReportTemplateUsageRepository extends JpaRepository<ReportTemplateUsage, Long> {
    boolean existsBySavedReportIdAndReportTemplateId(Long reportId, Long templateId);

}
