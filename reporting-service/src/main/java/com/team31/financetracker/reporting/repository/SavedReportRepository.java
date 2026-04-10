package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.SavedReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.team31.financetracker.reporting.model.ReportType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
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

    @Modifying
    @Transactional
    @Query(value = "UPDATE saved_reports SET status = 'ARCHIVED', " +
                   "report_config = report_config || jsonb_build_object('archiveReason', :reason, 'archivedAt', :at) " +
                   "WHERE id = :id AND status = 'GENERATED'", nativeQuery = true)
    int archiveReportNative(@Param("id") Long id, @Param("reason") String reason, @Param("at") String at);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)", nativeQuery = true)
    boolean existsUserById(@Param("userId") Long userId);

    @Query(value = "SELECT report_type, COUNT(*) FROM saved_reports " +
                   "WHERE user_id = :userId AND status = 'GENERATED' " +
                   "GROUP BY report_type", nativeQuery = true)
    List<Object[]> countGeneratedReportsByType(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
