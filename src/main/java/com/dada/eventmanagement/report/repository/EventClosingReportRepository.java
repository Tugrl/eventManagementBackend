package com.dada.eventmanagement.report.repository;

import com.dada.eventmanagement.report.entity.EventClosingReport;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventClosingReportRepository extends JpaRepository<EventClosingReport, Long> {
    Optional<EventClosingReport> findByCompanyIdAndEventId(Long companyId, Long eventId);
}
