package com.dada.eventmanagement.reservation.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.reservation.dto.ReservationUpsertRequest;
import com.dada.eventmanagement.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/api/reservations")
    public ApiResponse<?> listAll() {
        return ApiResponse.ok("Reservations listed", reservationService.listAll());
    }

    @GetMapping("/api/events/{eventId}/reservations")
    public ApiResponse<?> listByEvent(@PathVariable Long eventId) {
        return ApiResponse.ok("Reservations listed", reservationService.listByEvent(eventId));
    }

    @GetMapping("/api/reservations/{id}")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Reservation detail", reservationService.get(id));
    }

    @PostMapping("/api/events/{eventId}/reservations")
    public ApiResponse<?> create(@PathVariable Long eventId, @Valid @RequestBody ReservationUpsertRequest request) {
        return ApiResponse.ok("Reservation created", reservationService.create(eventId, request));
    }

    @PutMapping("/api/reservations/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody ReservationUpsertRequest request) {
        return ApiResponse.ok("Reservation updated", reservationService.update(id, request));
    }

    @DeleteMapping("/api/reservations/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ApiResponse.ok("Reservation cancelled");
    }

    @PatchMapping("/api/reservations/{id}/mark-deposit-paid")
    public ApiResponse<?> markDepositPaid(@PathVariable Long id) {
        return ApiResponse.ok("Reservation deposit marked paid", reservationService.markDepositPaid(id));
    }

    @PatchMapping("/api/reservations/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Reservation cancelled", reservationService.cancel(id));
    }
}
