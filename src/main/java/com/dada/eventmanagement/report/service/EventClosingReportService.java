package com.dada.eventmanagement.report.service;

import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.common.enums.ServicePayoutSource;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.cost.dto.EventCostRealizationResponse;
import com.dada.eventmanagement.cost.service.EventCostRealizationService;
import com.dada.eventmanagement.cost.service.EventCostService;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.finance.dto.FinanceDocumentResponse;
import com.dada.eventmanagement.finance.entity.FinanceDocument;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.repository.FinanceDocumentRepository;
import com.dada.eventmanagement.finance.repository.FinancialTransactionRepository;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.report.dto.ClosingReportRequest;
import com.dada.eventmanagement.report.dto.ClosingReportResponse;
import com.dada.eventmanagement.report.entity.EventClosingReport;
import com.dada.eventmanagement.report.repository.EventClosingReportRepository;
import com.dada.eventmanagement.reservation.entity.Reservation;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import com.dada.eventmanagement.staff.entity.ServicePayout;
import com.dada.eventmanagement.staff.entity.ServicePayoutItem;
import com.dada.eventmanagement.staff.repository.ServicePayoutItemRepository;
import com.dada.eventmanagement.staff.repository.ServicePayoutRepository;
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
    private final FinancialTransactionRepository financialTransactionRepository;
    private final FinanceDocumentRepository financeDocumentRepository;
    private final ReservationRepository reservationRepository;
    private final ServicePayoutRepository payoutRepository;
    private final ServicePayoutItemRepository payoutItemRepository;
    private final StaffService staffService;

    public EventClosingReportService(
            EventClosingReportRepository repository,
            EventService eventService,
            EventRepository eventRepository,
            EventCostService eventCostService,
            EventCostRealizationService realizationService,
            FinanceService financeService,
            FinancialTransactionRepository financialTransactionRepository,
            FinanceDocumentRepository financeDocumentRepository,
            ReservationRepository reservationRepository,
            ServicePayoutRepository payoutRepository,
            ServicePayoutItemRepository payoutItemRepository,
            StaffService staffService
    ) {
        this.repository = repository;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
        this.eventCostService = eventCostService;
        this.realizationService = realizationService;
        this.financeService = financeService;
        this.financialTransactionRepository = financialTransactionRepository;
        this.financeDocumentRepository = financeDocumentRepository;
        this.reservationRepository = reservationRepository;
        this.payoutRepository = payoutRepository;
        this.payoutItemRepository = payoutItemRepository;
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
        report.setActualTicketRevenue(numbers.collectedRevenueAmount());
        report.setActualDepositAmount(numbers.collectedRevenueAmount());
        report.setActualDoorPaymentAmount(BigDecimal.ZERO);
        report.setActualExtraSalesAmount(BigDecimal.ZERO);
        report.setActualTotalRevenue(numbers.collectedRevenueAmount());
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
                numbers.collectedRevenueAmount(),
                numbers.openReceivableAmount(),
                MathUtils.money(r.getActualTicketRevenue()),
                MathUtils.money(r.getActualDepositAmount()),
                MathUtils.money(r.getActualDoorPaymentAmount()),
                MathUtils.money(r.getActualExtraSalesAmount()),
                MathUtils.money(r.getActualTotalRevenue()),
                MathUtils.money(r.getActualTotalCost()),
                numbers.collectedRevenueAmount(),
                numbers.openReceivableAmount(),
                numbers.totalAccruedRevenueAmount(),
                numbers.paidCostAmount(),
                numbers.openPayableAmount(),
                numbers.totalAccruedCostAmount(),
                numbers.cashBasisProfit(),
                numbers.accrualBasisProfit(),
                MathUtils.money(numbers.servicePayoutTotal()),
                MathUtils.money(numbers.servicePayoutPaidAmount()),
                MathUtils.money(numbers.servicePayoutRemainingAmount()),
                MathUtils.money(numbers.servicePayoutDocumentBackedAmount()),
                numbers.servicePayoutOpenDocumentCount(),
                numbers.servicePayoutSettledDocumentCount(),
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

        RevenueSummary revenueSummary = revenueSummary(event);
        CostSummary costSummary = costSummary(event);
        ServicePayoutSummary servicePayoutSummary = servicePayoutSummary(event.getCompanyId(), event.getId());
        BigDecimal actualTotalCost = costSummary.totalAccruedCostAmount().add(servicePayoutSummary.servicePayoutTotal());
        BigDecimal actualProfit = MathUtils.money(revenueSummary.collectedRevenueAmount().subtract(actualTotalCost));

        return new ReportNumbers(
                actualGuestCount,
                finalTicketPrice,
                grossTicketPotential,
                revenueSummary.collectedRevenueAmount(),
                revenueSummary.openReceivableAmount(),
                revenueSummary.totalAccruedRevenueAmount(),
                costSummary.paidCostAmount(),
                costSummary.openPayableAmount(),
                costSummary.totalAccruedCostAmount(),
                MathUtils.money(revenueSummary.collectedRevenueAmount().subtract(costSummary.paidCostAmount())),
                MathUtils.money(revenueSummary.totalAccruedRevenueAmount().subtract(costSummary.totalAccruedCostAmount())),
                actualTotalCost,
                servicePayoutSummary.servicePayoutTotal(),
                servicePayoutSummary.servicePayoutPaidAmount(),
                servicePayoutSummary.servicePayoutRemainingAmount(),
                servicePayoutSummary.servicePayoutDocumentBackedAmount(),
                servicePayoutSummary.servicePayoutOpenDocumentCount(),
                servicePayoutSummary.servicePayoutSettledDocumentCount(),
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

    private RevenueSummary revenueSummary(Event event) {
        List<FinancialTransaction> transactions = financialTransactionRepository
                .findByCompanyIdAndEventIdAndTransactionTypeAndStatusOrderByTransactionDateDescIdDesc(
                        event.getCompanyId(),
                        event.getId(),
                        FinancialTransactionType.INCOME,
                        FinancialTransactionStatus.ACTIVE
                );
        BigDecimal directCollectedRevenue = transactions.stream()
                .filter(tx -> tx.getOperationSource() == OperationSource.EVENT_REVENUE)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FinanceDocument> documents = financeDocumentRepository
                .findByCompanyIdAndEventIdAndDocumentTypeAndStatusNotOrderByIssueDateDescIdDesc(
                        event.getCompanyId(),
                        event.getId(),
                        FinanceDocumentType.INCOME,
                        FinanceDocumentStatus.VOIDED
                );

        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = BigDecimal.ZERO;
        for (FinanceDocument document : documents) {
            FinanceDocumentResponse detail = financeService.documentDetail(document.getId());
            settledAmount = settledAmount.add(MathUtils.money(detail.settledAmount()));
            remainingAmount = remainingAmount.add(MathUtils.money(detail.remainingAmount()));
        }

        BigDecimal collectedRevenue = MathUtils.money(directCollectedRevenue.add(settledAmount));
        BigDecimal totalAccruedRevenue = MathUtils.money(collectedRevenue.add(remainingAmount));
        return new RevenueSummary(
                collectedRevenue,
                MathUtils.money(remainingAmount),
                totalAccruedRevenue
        );
    }

    private CostSummary costSummary(Event event) {
        List<EventCostRealizationResponse> rows = realizationService.list(event.getId());
        BigDecimal paidCost = BigDecimal.ZERO;
        BigDecimal openPayable = BigDecimal.ZERO;
        BigDecimal totalAccruedCost = BigDecimal.ZERO;

        for (EventCostRealizationResponse row : rows) {
            if (row.finalized() == null || !row.finalized()) {
                continue;
            }
            if (row.financeDocumentId() != null) {
                FinanceDocumentResponse detail = financeService.documentDetail(row.financeDocumentId());
                totalAccruedCost = totalAccruedCost.add(MathUtils.money(detail.totalAmount()));
                paidCost = paidCost.add(MathUtils.money(detail.settledAmount()));
                openPayable = openPayable.add(MathUtils.money(detail.remainingAmount()));
            } else {
                totalAccruedCost = totalAccruedCost.add(MathUtils.money(row.actualTotalCost()));
                if (row.paymentStatus() == CostPaymentStatus.PAID) {
                    paidCost = paidCost.add(MathUtils.money(row.actualTotalCost()));
                }
            }
        }

        return new CostSummary(
                MathUtils.money(paidCost),
                MathUtils.money(openPayable),
                MathUtils.money(totalAccruedCost)
        );
    }

    private ServicePayoutSummary servicePayoutSummary(Long companyId, Long eventId) {
        ServicePayout payout = payoutRepository.findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .filter(row -> row.getClosingStatus() != null && row.getClosingStatus() != com.dada.eventmanagement.common.enums.ServicePayoutClosingStatus.DRAFT)
                .orElse(null);
        if (payout == null) {
            return new ServicePayoutSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }

        List<ServicePayoutItem> items = payoutItemRepository.findByCompanyIdAndPayoutId(companyId, payout.getId());
        boolean hasDocumentBackedItems = items.stream().anyMatch(item -> item.getFinanceDocumentId() != null);
        if (hasDocumentBackedItems && items.stream().anyMatch(item -> item.getPayoutAmount().compareTo(BigDecimal.ZERO) > 0 && item.getFinanceDocumentId() == null)) {
            throw new BadRequestException("Servis hakedişi raporu belge uyumsuzluğu içeriyor");
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;
        BigDecimal documentBacked = BigDecimal.ZERO;
        int openCount = 0;
        int settledCount = 0;

        for (ServicePayoutItem item : items) {
            if (item.getPayoutAmount() == null || item.getPayoutAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total = total.add(item.getPayoutAmount());
            if (item.getFinanceDocumentId() != null) {
                FinanceDocumentResponse document = financeService.documentDetail(item.getFinanceDocumentId());
                if (document.status() != FinanceDocumentStatus.VOIDED) {
                    documentBacked = documentBacked.add(item.getPayoutAmount());
                    paid = paid.add(MathUtils.money(document.settledAmount()));
                    remaining = remaining.add(MathUtils.money(document.remainingAmount()));
                    if (document.status() == FinanceDocumentStatus.SETTLED) {
                        settledCount++;
                    } else if (document.status() == FinanceDocumentStatus.OPEN || document.status() == FinanceDocumentStatus.PARTIALLY_SETTLED) {
                        openCount++;
                    }
                }
            } else {
                if (payout.getPaymentStatus() == CostPaymentStatus.PAID) {
                    paid = paid.add(item.getPayoutAmount());
                } else {
                    remaining = remaining.add(item.getPayoutAmount());
                    openCount++;
                }
            }
        }

        return new ServicePayoutSummary(total, paid, remaining, documentBacked, openCount, settledCount);
    }

    private record ReportNumbers(
            Integer actualGuestCount,
            BigDecimal finalTicketPrice,
            BigDecimal grossTicketPotential,
            BigDecimal collectedRevenueAmount,
            BigDecimal openReceivableAmount,
            BigDecimal totalAccruedRevenueAmount,
            BigDecimal paidCostAmount,
            BigDecimal openPayableAmount,
            BigDecimal totalAccruedCostAmount,
            BigDecimal cashBasisProfit,
            BigDecimal accrualBasisProfit,
            BigDecimal actualTotalCost,
            BigDecimal servicePayoutTotal,
            BigDecimal servicePayoutPaidAmount,
            BigDecimal servicePayoutRemainingAmount,
            BigDecimal servicePayoutDocumentBackedAmount,
            Integer servicePayoutOpenDocumentCount,
            Integer servicePayoutSettledDocumentCount,
            BigDecimal actualProfit
    ) {
    }

    private record RevenueSummary(
            BigDecimal collectedRevenueAmount,
            BigDecimal openReceivableAmount,
            BigDecimal totalAccruedRevenueAmount
    ) {
    }

    private record CostSummary(
            BigDecimal paidCostAmount,
            BigDecimal openPayableAmount,
            BigDecimal totalAccruedCostAmount
    ) {
    }

    private record ServicePayoutSummary(
            BigDecimal servicePayoutTotal,
            BigDecimal servicePayoutPaidAmount,
            BigDecimal servicePayoutRemainingAmount,
            BigDecimal servicePayoutDocumentBackedAmount,
            Integer servicePayoutOpenDocumentCount,
            Integer servicePayoutSettledDocumentCount
    ) {
    }
}
