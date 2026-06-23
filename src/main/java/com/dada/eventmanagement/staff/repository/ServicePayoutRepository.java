package com.dada.eventmanagement.staff.repository;

import com.dada.eventmanagement.staff.entity.ServicePayout;
import com.dada.eventmanagement.common.enums.ServicePayoutSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePayoutRepository extends JpaRepository<ServicePayout, Long> {
    List<ServicePayout> findByCompanyIdAndPayoutDateBetweenOrderByPayoutDateDesc(Long companyId, LocalDate startDate, LocalDate endDate);
    Optional<ServicePayout> findByIdAndCompanyId(Long id, Long companyId);
    Optional<ServicePayout> findByCompanyIdAndEventIdAndPayoutSource(Long companyId, Long eventId, ServicePayoutSource payoutSource);
}
