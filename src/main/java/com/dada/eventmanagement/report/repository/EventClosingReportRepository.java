package com.dada.eventmanagement.report.repository;

import com.dada.eventmanagement.report.entity.EventClosingReport;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventClosingReportRepository extends JpaRepository<EventClosingReport, Long> {
    Optional<EventClosingReport> findByCompanyIdAndEventId(Long companyId, Long eventId);
    List<EventClosingReport> findByCompanyIdAndEventIdIn(Long companyId, Collection<Long> eventIds);
}
