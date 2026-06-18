package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.finance.entity.DailyCashReport;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCashReportRepository extends JpaRepository<DailyCashReport, Long> {
    Optional<DailyCashReport> findByCompanyIdAndReportDate(Long companyId, LocalDate reportDate);
    Optional<DailyCashReport> findByIdAndCompanyId(Long id, Long companyId);
    List<DailyCashReport> findByCompanyIdAndReportDateBetweenOrderByReportDateDesc(Long companyId, LocalDate startDate, LocalDate endDate);
}
