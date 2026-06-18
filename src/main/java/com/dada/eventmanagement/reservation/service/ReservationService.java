package com.dada.eventmanagement.reservation.service;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.ReservationStatus;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.payment.repository.ReservationPaymentRepository;
import com.dada.eventmanagement.reservation.dto.ReservationResponse;
import com.dada.eventmanagement.reservation.dto.ReservationUpsertRequest;
import com.dada.eventmanagement.reservation.entity.Reservation;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final EventService eventService;
    private final ReservationPaymentRepository paymentRepository;

    public ReservationService(ReservationRepository reservationRepository, EventService eventService, ReservationPaymentRepository paymentRepository) {
        this.reservationRepository = reservationRepository;
        this.eventService = eventService;
        this.paymentRepository = paymentRepository;
    }

    public List<ReservationResponse> listByEvent(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return reservationRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), eventId)
                .stream().map(this::toResponse).toList();
    }

    public List<ReservationResponse> listAll() {
        Long companyId = SecurityUtils.currentCompanyId();
        return reservationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ReservationResponse create(Long eventId, ReservationUpsertRequest request) {
        Event event = eventService.findEvent(eventId);
        eventService.validateReservationReady(event);
        Reservation reservation = new Reservation();
        reservation.setCompanyId(event.getCompanyId());
        reservation.setEventId(eventId);
        apply(reservation, request);
        reservation.setReservationCode(generateCode(event.getCompanyId()));
        reservation.setDepositStatus(DepositStatus.PENDING);
        reservation.setReservationStatus(ReservationStatus.ACTIVE);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse update(Long id, ReservationUpsertRequest request) {
        Reservation reservation = find(id);
        apply(reservation, request);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        Reservation reservation = find(id);
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Transactional
    public ReservationResponse markDepositPaid(Long id) {
        Reservation reservation = find(id);
        reservation.setDepositStatus(DepositStatus.PAID);
        return toResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = find(id);
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        return toResponse(reservationRepository.save(reservation));
    }

    public Reservation find(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        return reservationRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    private void apply(Reservation reservation, ReservationUpsertRequest request) {
        reservation.setCustomerName(request.customerName().trim());
        reservation.setCustomerPhone(request.customerPhone().trim());
        reservation.setCustomerEmail(request.customerEmail());
        reservation.setGuestCount(request.guestCount());
        reservation.setTableNumber(request.tableNumber());
        reservation.setDepositAmount(MathUtils.money(request.depositAmount()));
        reservation.setNotes(request.notes());
    }

    private String generateCode(Long companyId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "REZ-" + datePart + "-";
        long count = reservationRepository.countByCompanyIdAndReservationCodeStartingWith(companyId, prefix);
        return prefix + String.format("%04d", count + 1);
    }

    private ReservationResponse toResponse(Reservation r) {
        Event event = eventService.findEvent(r.getEventId());
        BigDecimal finalTicketPrice = MathUtils.money(event.getFinalTicketPrice());
        BigDecimal totalTicketAmount = MathUtils.money(finalTicketPrice.multiply(BigDecimal.valueOf(r.getGuestCount())));
        BigDecimal paidAmount = MathUtils.money(paymentRepository.sumByCompanyAndReservationId(r.getCompanyId(), r.getId()));
        BigDecimal remainingAmount = MathUtils.money(totalTicketAmount.subtract(paidAmount).max(BigDecimal.ZERO));
        return new ReservationResponse(
                r.getId(),
                r.getEventId(),
                event.getTitle(),
                event.getEventDate(),
                r.getReservationCode(),
                r.getCustomerName(),
                r.getCustomerPhone(),
                r.getCustomerEmail(),
                r.getGuestCount(),
                r.getTableNumber(),
                MathUtils.money(r.getDepositAmount()),
                finalTicketPrice,
                totalTicketAmount,
                paidAmount,
                remainingAmount,
                r.getDepositStatus(),
                r.getReservationStatus(),
                r.getNotes(),
                r.getCreatedAt()
        );
    }
}
