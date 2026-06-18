package com.dada.eventmanagement.inventory.repository;

import com.dada.eventmanagement.inventory.entity.InventoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    Optional<InventoryItem> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
}
