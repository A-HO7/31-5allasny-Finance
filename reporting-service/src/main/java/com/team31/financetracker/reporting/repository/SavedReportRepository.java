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
    @Query("SELECT r FROM SavedReport r WHERE " +
           "(:hasType = false OR r.reportType = :type) AND " +
           "(:hasStatus = false OR r.status = :status) AND " +
           "(:hasStart = false OR r.periodStart >= :startDate) AND " +
           "(:hasEnd = false OR r.periodEnd <= :endDate) " +
           "ORDER BY r.createdAt DESC")
    List<SavedReport> searchReports(
            @Param("hasType") boolean hasType,
            @Param("type") ReportType type,
            @Param("hasStatus") boolean hasStatus,
            @Param("status") ReportStatus status,
            @Param("hasStart") boolean hasStart,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("hasEnd") boolean hasEnd,
            @Param("endDate") java.time.LocalDate endDate);




    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    @Query("SELECT cast(r.reportType as string) as reportType, CAST(COUNT(r) as int) as count FROM SavedReport r " +
           "WHERE r.userId = :userId " +
           "GROUP BY r.reportType")
    List<com.team31.financetracker.reporting.dto.ReportTypeBreakdownProjection> countGeneratedReportsByType(@Param("userId") Long userId);

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
        "WHERE (:startDate IS NULL OR period_start >= CAST(:startDate AS DATE)) " +
        "  AND (:endDate IS NULL OR period_end <= CAST(:endDate AS DATE))",
        nativeQuery = true)
       List<Object[]> getReportAnalytics(@Param("startDate") String startDate,
                                      @Param("endDate") String endDate);
}
