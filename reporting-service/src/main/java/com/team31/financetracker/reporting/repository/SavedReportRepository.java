package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team31.financetracker.reporting.model.ReportType;
import com.team31.financetracker.reporting.model.ReportStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SavedReportRepository extends JpaRepository<SavedReport, Long> {
    @Query(value = "SELECT * FROM saved_reports WHERE " +
           "(CAST(:reportType AS VARCHAR) IS NULL OR report_type = CAST(:reportType AS VARCHAR)) AND " +
           "(CAST(:start AS TIMESTAMP) IS NULL OR created_at >= CAST(:start AS TIMESTAMP)) AND " +
           "(CAST(:end AS TIMESTAMP) IS NULL OR created_at < CAST(:end AS TIMESTAMP)) " +
           "ORDER BY created_at DESC", 
           nativeQuery = true)
    List<SavedReport> searchReportsNative(@Param("reportType") String reportType,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);



    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    @Query(value = "SELECT report_type, COUNT(*) FROM saved_reports " +
                   "WHERE user_id = :userId AND status = 'GENERATED' " +
                   "GROUP BY report_type", nativeQuery = true)
    List<Object[]> countGeneratedReportsByType(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(r) > 0 FROM SavedReport r " +
           "WHERE r.userId = :userId " +
           "AND r.reportType = :reportType " +
           "AND r.status = com.team31.financetracker.reporting.model.ReportStatus.GENERATED " +
           "AND r.periodStart <= :periodEnd " +
           "AND r.periodEnd >= :periodStart")
    boolean existsOverlappingGeneratedReport(@Param("userId") Long userId,
                                             @Param("reportType") ReportType reportType,
                                             @Param("periodStart") java.time.LocalDate periodStart,
                                             @Param("periodEnd") java.time.LocalDate periodEnd);

    @Query(value =
        "SELECT " +
        "  COUNT(*) AS totalReports, " +
        "  COUNT(CASE WHEN status = 'GENERATED' THEN 1 END) AS totalGenerated, " +
        "  COALESCE(AVG(CASE WHEN status = 'GENERATED' " +
        "    THEN (period_end - period_start) END), 0) AS averagePeriodDays, " +
        "  COUNT(CASE WHEN status = 'ARCHIVED' THEN 1 END) AS archivedCount, " +
        "  COUNT(CASE WHEN status = 'FAILED' THEN 1 END) AS failedCount " +
        "FROM saved_reports " +
        "WHERE (CAST(:startDate AS TIMESTAMP) IS NULL OR created_at >= CAST(:startDate AS TIMESTAMP)) " +
        "  AND (CAST(:endDate AS TIMESTAMP) IS NULL OR created_at <= CAST(:endDate AS TIMESTAMP))",
        nativeQuery = true)
    List<Object[]> getReportAnalytics(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
