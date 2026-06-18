package com.dada.eventmanagement.pricing.service;

import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.cost.service.EventCostService;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.pricing.dto.FinalTicketPriceRequest;
import com.dada.eventmanagement.pricing.dto.TicketPriceSuggestionResponse;
import com.dada.eventmanagement.pricing.entity.TicketPriceSuggestion;
import com.dada.eventmanagement.pricing.repository.TicketPriceSuggestionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {
    private final TicketPriceSuggestionRepository suggestionRepository;
    private final EventCostService eventCostService;
    private final EventService eventService;
    private final EventRepository eventRepository;

    public PricingService(TicketPriceSuggestionRepository suggestionRepository, EventCostService eventCostService, EventService eventService, EventRepository eventRepository) {
        this.suggestionRepository = suggestionRepository;
        this.eventCostService = eventCostService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public TicketPriceSuggestionResponse createSuggestion(Long eventId) {
        Event event = eventService.findEvent(eventId);
        eventService.validatePricingAllowed(event);
        BigDecimal totalEstimatedCost = eventCostService.totalEstimatedCost(eventId);
        BigDecimal margin = MathUtils.money(event.getTargetProfitMargin());
        BigDecimal targetRevenue = totalEstimatedCost.add(totalEstimatedCost.multiply(margin).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        BigDecimal suggested = event.getExpectedGuestCount() == 0
                ? BigDecimal.ZERO
                : targetRevenue.divide(BigDecimal.valueOf(event.getExpectedGuestCount()), 2, RoundingMode.HALF_UP);

        TicketPriceSuggestion suggestion = new TicketPriceSuggestion();
        suggestion.setCompanyId(event.getCompanyId());
        suggestion.setEventId(eventId);
        suggestion.setTotalEstimatedCost(totalEstimatedCost);
        suggestion.setTargetProfitMargin(margin);
        suggestion.setTargetRevenue(MathUtils.money(targetRevenue));
        suggestion.setExpectedGuestCount(event.getExpectedGuestCount());
        suggestion.setSuggestedTicketPrice(MathUtils.money(suggested));
        TicketPriceSuggestion saved = suggestionRepository.save(suggestion);

        event.setSuggestedTicketPrice(saved.getSuggestedTicketPrice());
        event.setTargetRevenueAmount(saved.getTargetRevenue());
        event.setTargetProfitAmount(saved.getTargetRevenue().subtract(totalEstimatedCost));
        eventRepository.save(event);
        return toResponse(saved);
    }

    public List<TicketPriceSuggestionResponse> listSuggestions(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return suggestionRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void updateFinalTicketPrice(Long eventId, FinalTicketPriceRequest request) {
        Event event = eventService.findEvent(eventId);
        eventService.validatePricingAllowed(event);
        event.setFinalTicketPrice(MathUtils.money(request.finalTicketPrice()));
        event.setPricingFinalized(true);
        event.setStatus(EventStatus.ON_SALE);
        eventRepository.save(event);
    }

    private TicketPriceSuggestionResponse toResponse(TicketPriceSuggestion s) {
        return new TicketPriceSuggestionResponse(
                s.getId(),
                s.getEventId(),
                MathUtils.money(s.getTotalEstimatedCost()),
                MathUtils.money(s.getTargetProfitMargin()),
                MathUtils.money(s.getTargetRevenue()),
                s.getExpectedGuestCount(),
                MathUtils.money(s.getSuggestedTicketPrice()),
                s.getCreatedAt()
        );
    }
}
