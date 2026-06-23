package com.dada.eventmanagement.cost.repository;

import com.dada.eventmanagement.cost.entity.EventCostRealization;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventCostRealizationRepository extends JpaRepository<EventCostRealization, Long> {
    List<EventCostRealization> findByCompanyIdAndEventIdOrderByCreatedAtAsc(Long companyId, Long eventId);
    Optional<EventCostRealization> findByIdAndCompanyIdAndEventId(Long id, Long companyId, Long eventId);
    Optional<EventCostRealization> findByCompanyIdAndEventIdAndEventCostId(Long companyId, Long eventId, Long eventCostId);
    Optional<EventCostRealization> findByCompanyIdAndEventIdAndInventoryItemId(Long companyId, Long eventId, Long inventoryItemId);

    @Query("select coalesce(sum(r.actualTotalCost), 0) from EventCostRealization r where r.companyId = :companyId and r.eventId = :eventId and r.finalized = true and r.verificationStatus <> 'NOT_INCURRED'")
    BigDecimal sumFinalizedActualCost(Long companyId, Long eventId);
}
