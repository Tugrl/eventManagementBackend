package com.dada.eventmanagement.cost.repository;

import com.dada.eventmanagement.common.enums.CalculationType;
import com.dada.eventmanagement.cost.entity.EventCost;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventCostRepository extends JpaRepository<EventCost, Long> {
    List<EventCost> findByCompanyIdAndEventIdOrderByCreatedAtDesc(Long companyId, Long eventId);
    Optional<EventCost> findByIdAndCompanyId(Long id, Long companyId);
    Optional<EventCost> findByCompanyIdAndEventIdAndName(Long companyId, Long eventId, String name);
    List<EventCost> findByCompanyIdAndEventIdAndNameStartingWith(Long companyId, Long eventId, String namePrefix);

    @Query("select coalesce(sum(ec.totalCost), 0) from EventCost ec where ec.companyId = :companyId and ec.eventId = :eventId")
    BigDecimal sumEstimatedCostByEvent(Long companyId, Long eventId);

    @Query("select coalesce(sum(ec.totalCost), 0) from EventCost ec where ec.companyId = :companyId")
    BigDecimal sumEstimatedCostByCompany(Long companyId);

    @Query("select coalesce(sum(ec.totalCost), 0) from EventCost ec where ec.companyId = :companyId and ec.eventId = :eventId and ec.calculationType = :type")
    BigDecimal sumByType(Long companyId, Long eventId, CalculationType type);
}
