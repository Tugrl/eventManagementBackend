package com.dada.eventmanagement.staff.repository;

import com.dada.eventmanagement.staff.entity.ServicePayoutItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicePayoutItemRepository extends JpaRepository<ServicePayoutItem, Long> {
    List<ServicePayoutItem> findByCompanyIdAndPayoutId(Long companyId, Long payoutId);
    void deleteByCompanyIdAndPayoutId(Long companyId, Long payoutId);
}
