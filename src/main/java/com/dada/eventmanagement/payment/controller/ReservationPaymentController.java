package com.dada.eventmanagement.payment.controller;

import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.payment.dto.ReservationPaymentRequest;
import com.dada.eventmanagement.payment.service.ReservationPaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReservationPaymentController {
    private final ReservationPaymentService paymentService;

    public ReservationPaymentController(ReservationPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/api/reservations/{reservationId}/payments")
    public ApiResponse<?> list(@PathVariable Long reservationId) {
        return ApiResponse.ok("Reservation payments listed", paymentService.list(reservationId));
    }

    @PostMapping("/api/reservations/{reservationId}/payments")
    public ApiResponse<?> create(@PathVariable Long reservationId, @Valid @RequestBody ReservationPaymentRequest request) {
        return ApiResponse.ok("Reservation payment created", paymentService.create(reservationId, request));
    }

    @DeleteMapping("/api/reservation-payments/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ApiResponse.ok("Reservation payment deleted");
    }
}
