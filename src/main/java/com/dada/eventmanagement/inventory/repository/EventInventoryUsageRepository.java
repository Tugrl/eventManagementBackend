package com.dada.eventmanagement.inventory.repository;

import com.dada.eventmanagement.inventory.entity.EventInventoryUsage;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventInventoryUsageRepository extends JpaRepository<EventInventoryUsage, Long> {
    List<EventInventoryUsage> findByCompanyIdAndEventIdOrderByUsageDateDescIdDesc(Long companyId, Long eventId);
    List<EventInventoryUsage> findByCompanyIdAndUsageDateBetweenOrderByUsageDateDescIdDesc(Long companyId, LocalDate startDate, LocalDate endDate);
}
