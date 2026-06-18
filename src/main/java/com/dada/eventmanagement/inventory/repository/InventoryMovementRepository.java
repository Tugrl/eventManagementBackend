package com.dada.eventmanagement.inventory.repository;

import com.dada.eventmanagement.inventory.entity.InventoryMovement;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByCompanyIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(Long companyId, LocalDate startDate, LocalDate endDate);
    List<InventoryMovement> findByCompanyIdAndInventoryItemIdOrderByMovementDateDescIdDesc(Long companyId, Long inventoryItemId);
}
