package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.ReportTemplateUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportTemplateUsageRepository extends JpaRepository<ReportTemplateUsage, Long> {

    @Query("SELECT u FROM ReportTemplateUsage u LEFT JOIN FETCH u.savedReport LEFT JOIN FETCH u.reportTemplate WHERE u.id = :id")
    Optional<ReportTemplateUsage> findByIdWithRelations(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReportTemplateUsage u WHERE u.id = :id")
    void deleteByIdCustom(@Param("id") Long id);

    boolean existsBySavedReportIdAndReportTemplateId(Long reportId, Long templateId);
}
