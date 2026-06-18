package com.dada.eventmanagement.event.repository;

import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.event.entity.Event;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCompanyIdAndIsDeletedFalseOrderByEventDateAsc(Long companyId);
    Optional<Event> findByIdAndCompanyIdAndIsDeletedFalse(Long id, Long companyId);
    long countByCompanyIdAndIsDeletedFalse(Long companyId);
    long countByCompanyIdAndStatusAndIsDeletedFalse(Long companyId, EventStatus status);
    long countByCompanyIdAndEventDateGreaterThanEqualAndIsDeletedFalse(Long companyId, LocalDate date);
    List<Event> findTop5ByCompanyIdAndEventDateGreaterThanEqualAndIsDeletedFalseOrderByEventDateAsc(Long companyId, LocalDate date);
    List<Event> findByCompanyIdAndEventDateAndIsDeletedFalse(Long companyId, LocalDate eventDate);

    @Query("""
            select coalesce(sum(e.expectedGuestCount * coalesce(e.finalTicketPrice, 0)), 0)
            from Event e
            where e.companyId = :companyId and e.isDeleted = false
            """)
    java.math.BigDecimal sumEstimatedRevenue(Long companyId);
}
