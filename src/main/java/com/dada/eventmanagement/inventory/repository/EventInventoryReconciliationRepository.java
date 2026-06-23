package com.dada.eventmanagement.inventory.repository;

import com.dada.eventmanagement.inventory.entity.EventInventoryReconciliation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventInventoryReconciliationRepository extends JpaRepository<EventInventoryReconciliation, Long> {
    List<EventInventoryReconciliation> findByCompanyIdAndEventIdOrderByCreatedAtAsc(Long companyId, Long eventId);
    Optional<EventInventoryReconciliation> findByIdAndCompanyIdAndEventId(Long id, Long companyId, Long eventId);
    Optional<EventInventoryReconciliation> findByCompanyIdAndEventIdAndInventoryItemId(Long companyId, Long eventId, Long inventoryItemId);
}
