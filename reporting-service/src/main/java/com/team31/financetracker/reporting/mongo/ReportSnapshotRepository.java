package com.team31.financetracker.reporting.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReportSnapshotRepository extends MongoRepository<ReportSnapshot, String> {
    List<ReportSnapshot> findByReportIdOrderBySnapshotCreatedAtDesc(Long reportId);
}