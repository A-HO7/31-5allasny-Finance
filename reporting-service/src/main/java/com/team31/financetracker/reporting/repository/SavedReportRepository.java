package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team31.financetracker.reporting.model.ReportType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {

    @Query("SELECT r FROM SavedReport r WHERE " +
           "(:reportType IS NULL OR r.reportType = :reportType) AND " +
           "(cast(:startDate as timestamp) IS NULL OR r.createdAt >= :startDate) AND " +
           "(cast(:endDate as timestamp) IS NULL OR r.createdAt <= :endDate) " +
           "ORDER BY r.createdAt DESC")
    List<SavedReport> searchReports(@Param("reportType") ReportType reportType,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);
}
