package com.dada.eventmanagement.pricing.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.pricing.dto.FinalTicketPriceRequest;
import com.dada.eventmanagement.pricing.service.PricingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class PricingController {
    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/api/events/{eventId}/ticket-price-suggestion")
    public ApiResponse<?> createSuggestion(@PathVariable Long eventId) {
        return ApiResponse.ok("Ticket price suggestion created", pricingService.createSuggestion(eventId));
    }

    @GetMapping("/api/events/{eventId}/ticket-price-suggestions")
    public ApiResponse<?> listSuggestions(@PathVariable Long eventId) {
        return ApiResponse.ok("Ticket price suggestions listed", pricingService.listSuggestions(eventId));
    }

    @PatchMapping("/api/events/{eventId}/final-ticket-price")
    public ApiResponse<?> updateFinalTicketPrice(@PathVariable Long eventId, @Valid @RequestBody FinalTicketPriceRequest request) {
        pricingService.updateFinalTicketPrice(eventId, request);
        return ApiResponse.ok("Final ticket price updated");
    }
}
