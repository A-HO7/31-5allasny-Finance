package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {
}
