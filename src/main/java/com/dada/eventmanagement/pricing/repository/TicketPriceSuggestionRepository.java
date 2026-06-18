package com.dada.eventmanagement.pricing.repository;

import com.dada.eventmanagement.pricing.entity.TicketPriceSuggestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketPriceSuggestionRepository extends JpaRepository<TicketPriceSuggestion, Long> {
    List<TicketPriceSuggestion> findByCompanyIdAndEventIdOrderByCreatedAtDesc(Long companyId, Long eventId);
}
