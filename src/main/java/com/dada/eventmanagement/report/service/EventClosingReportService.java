package com.dada.eventmanagement.report.service;

import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.cost.service.EventCostService;
import com.dada.eventmanagement.cost.service.EventCostRealizationService;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.report.dto.ClosingReportRequest;
import com.dada.eventmanagement.report.dto.ClosingReportResponse;
import com.dada.eventmanagement.report.entity.EventClosingReport;
import com.dada.eventmanagement.report.repository.EventClosingReportRepository;
import com.dada.eventmanagement.reservation.entity.Reservation;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import com.dada.eventmanagement.staff.service.StaffService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventClosingReportService {
    private final EventClosingReportRepository repository;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final EventCostService eventCostService;
    private final EventCostRealizationService realizationService;
    private final FinanceService financeService;
    private final ReservationRepository reservationRepository;
    private final StaffService staffService;

    public EventClosingReportService(EventClosingReportRepository repository, EventService eventService, EventRepository eventRepository, EventCostService eventCostService, EventCostRealizationService realizationService, FinanceService financeService, ReservationRepository reservationRepository, StaffService staffService) {
        this.repository = repository;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.eventCostService = eventCostService;
        this.realizationService = realizationService;
        this.financeService = financeService;
        this.reservationRepository = reservationRepository;
        this.staffService = staffService;
    }

    @Transactional
    public ClosingReportResponse create(Long eventId, ClosingReportRequest request) {
        Event event = eventService.findEvent(eventId);
        realizationService.validateFinalized(eventId);
        staffService.validateClosingPayoutFinalized(eventId);
        EventClosingReport report = repository.findByCompanyIdAndEventId(event.getCompanyId(), eventId).orElse(new EventClosingReport());
        report.setCompanyId(event.getCompanyId());
        report.setEventId(eventId);
        apply(report, request, event);
        EventClosingReport saved = repository.save(report);
        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);
        return toResponse(saved);
    }

    public ClosingReportResponse get(Long eventId) {
        Event event = eventService.findEvent(eventId);
        EventClosingReport report = repository.findByCompanyIdAndEventId(event.getCompanyId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Closing report not found"));
        return toResponse(report);
    }

    @Transactional
    public ClosingReportResponse update(Long eventId, ClosingReportRequest request) {
        Event event = eventService.findEvent(eventId);
        realizationService.validateFinalized(eventId);
        staffService.validateClosingPayoutFinalized(eventId);
        EventClosingReport report = repository.findByCompanyIdAndEventId(event.getCompanyId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Closing report not found"));
        apply(report, request, event);
        return toResponse(repository.save(report));
    }

    private void apply(EventClosingReport report, ClosingReportRequest request, Event event) {
        ReportNumbers numbers = calculate(event);
        BigDecimal estimatedRevenue = estimatedRevenue(event);
        BigDecimal estimatedCost = eventCostService.totalEstimatedCost(event.getId());
        BigDecimal estimatedProfit = estimatedRevenue.subtract(estimatedCost);
        BigDecimal difference = numbers.actualProfit().subtract(estimatedProfit);

        report.setActualGuestCount(numbers.actualGuestCount());
        report.setActualTicketRevenue(numbers.collectedPaymentAmount());
        report.setActualDepositAmount(numbers.collectedPaymentAmount());
        report.setActualDoorPaymentAmount(BigDecimal.ZERO);
        report.setActualExtraSalesAmount(BigDecimal.ZERO);
        report.setActualTotalRevenue(numbers.collectedPaymentAmount());
        report.setActualTotalCost(numbers.actualTotalCost());
        report.setActualProfit(numbers.actualProfit());
        report.setEstimatedTotalRevenue(MathUtils.money(estimatedRevenue));
        report.setEstimatedTotalCost(MathUtils.money(estimatedCost));
        report.setEstimatedProfit(MathUtils.money(estimatedProfit));
        report.setProfitDifference(MathUtils.money(difference));
        report.setNotes(request == null ? null : request.notes());
    }

    private ClosingReportResponse toResponse(EventClosingReport r) {
        Event event = eventService.findEvent(r.getEventId());
        ReportNumbers numbers = calculate(event);
        return new ClosingReportResponse(
                r.getId(),
                r.getEventId(),
                r.getActualGuestCount(),
                numbers.finalTicketPrice(),
                numbers.grossTicketPotential(),
                numbers.collectedPaymentAmount(),
                numbers.remainingReceivableAmount(),
                MathUtils.money(r.getActualTicketRevenue()),
                MathUtils.money(r.getActualDepositAmount()),
                MathUtils.money(r.getActualDoorPaymentAmount()),
                MathUtils.money(r.getActualExtraSalesAmount()),
                MathUtils.money(r.getActualTotalRevenue()),
                MathUtils.money(r.getActualTotalCost()),
                MathUtils.money(r.getActualProfit()),
                MathUtils.money(r.getEstimatedTotalRevenue()),
                MathUtils.money(r.getEstimatedTotalCost()),
                MathUtils.money(r.getEstimatedProfit()),
                MathUtils.money(r.getProfitDifference()),
                r.getNotes(),
                r.getUpdatedAt()
        );
    }

    private ReportNumbers calculate(Event event) {
        List<Reservation> reservations = reservationRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), event.getId())
                .stream()
                .filter(r -> r.getReservationStatus() == com.dada.eventmanagement.common.enums.ReservationStatus.ACTIVE
                        || r.getReservationStatus() == com.dada.eventmanagement.common.enums.ReservationStatus.COMPLETED)
                .toList();
        int actualGuestCount = reservations.stream().mapToInt(Reservation::getGuestCount).sum();
        BigDecimal finalTicketPrice = MathUtils.money(event.getFinalTicketPrice());
        BigDecimal grossTicketPotential = estimatedRevenue(event);
        BigDecimal collectedPaymentAmount = MathUtils.money(financeService.eventIncomeTransactions(event.getId()).stream()
                .map(row -> row.amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal remainingReceivableAmount = MathUtils.money(grossTicketPotential.subtract(collectedPaymentAmount).max(BigDecimal.ZERO));
        BigDecimal actualTotalCost = realizationService.totalFinalizedActualCost(event.getId())
                .add(staffService.totalFinalizedServiceCost(event.getId()));
        BigDecimal actualProfit = MathUtils.money(collectedPaymentAmount.subtract(actualTotalCost));
        return new ReportNumbers(
                actualGuestCount,
                finalTicketPrice,
                grossTicketPotential,
                collectedPaymentAmount,
                remainingReceivableAmount,
                actualTotalCost,
                actualProfit
        );
    }

    private BigDecimal estimatedRevenue(Event event) {
        BigDecimal target = MathUtils.money(event.getTargetRevenueAmount());
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            return target;
        }
        return MathUtils.money(Optional.ofNullable(event.getFinalTicketPrice()).orElse(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(event.getExpectedGuestCount())));
    }

    private record ReportNumbers(
            Integer actualGuestCount,
            BigDecimal finalTicketPrice,
            BigDecimal grossTicketPotential,
            BigDecimal collectedPaymentAmount,
            BigDecimal remainingReceivableAmount,
            BigDecimal actualTotalCost,
            BigDecimal actualProfit
    ) {
    }
}
