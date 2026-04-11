package com.team31.financetracker.reporting.repository;

import com.team31.financetracker.reporting.model.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, Long> {
    boolean existsByCode(String code);

    @Query(value = "SELECT rt.id, rt.code, rt.template_type, rt.max_pages, rt.current_uses, " +
            "COALESCE(SUM(rtu.pages_generated), 0) as total_pages, rt.active, rt.expiry_date " +
            "FROM report_templates rt " +
            "LEFT JOIN report_template_usages rtu ON rt.id = rtu.template_id " +
            "GROUP BY rt.id, rt.code, rt.template_type, rt.max_pages, rt.current_uses, rt.active, rt.expiry_date " +
            "ORDER BY rt.current_uses DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopUsedTemplates(@Param("limit") int limit);

}
