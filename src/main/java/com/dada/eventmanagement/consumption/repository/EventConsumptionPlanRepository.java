package com.dada.eventmanagement.consumption.repository;

import com.dada.eventmanagement.consumption.entity.EventConsumptionPlan;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventConsumptionPlanRepository extends JpaRepository<EventConsumptionPlan, Long> {
    List<EventConsumptionPlan> findByCompanyIdAndEventIdOrderByCreatedAtDesc(Long companyId, Long eventId);
    Optional<EventConsumptionPlan> findByIdAndCompanyId(Long id, Long companyId);

    @Query("select coalesce(sum(cp.totalCost), 0) from EventConsumptionPlan cp where cp.companyId = :companyId and cp.eventId = :eventId")
    BigDecimal sumByEvent(Long companyId, Long eventId);
}
