package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportTemplateUsageRepository extends JpaRepository<ReportTemplateUsage, Long> {
}
