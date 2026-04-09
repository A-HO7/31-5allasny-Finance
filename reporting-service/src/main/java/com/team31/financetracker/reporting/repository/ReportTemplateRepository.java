package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
}
