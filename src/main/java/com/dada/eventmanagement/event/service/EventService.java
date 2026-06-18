package com.dada.eventmanagement.event.service;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.EventRevenueModel;
import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.common.enums.ReservationStatus;
import com.dada.eventmanagement.common.enums.CalculationType;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.consumption.repository.EventConsumptionPlanRepository;
import com.dada.eventmanagement.consumption.entity.EventConsumptionPlan;
import com.dada.eventmanagement.cost.entity.CostCategory;
import com.dada.eventmanagement.cost.entity.EventCost;
import com.dada.eventmanagement.cost.repository.CostCategoryRepository;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.dto.EventResponse;
import com.dada.eventmanagement.event.dto.EventSummaryResponse;
import com.dada.eventmanagement.event.dto.EventUpsertRequest;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.payment.repository.ReservationPaymentRepository;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
    private static final String CONSUMPTION_COST_NAME = "Tüketim Planı Toplam Maliyeti";
    private static final String CONSUMPTION_COST_PREFIX = "Tüketim Planı - ";
    private static final String CONSUMPTION_CATEGORY_NAME = "Tüketim Planı";

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository paymentRepository;
    private final EventCostRepository eventCostRepository;
    private final EventConsumptionPlanRepository consumptionPlanRepository;
    private final CostCategoryRepository costCategoryRepository;

    public EventService(EventRepository eventRepository, ReservationRepository reservationRepository, ReservationPaymentRepository paymentRepository, EventCostRepository eventCostRepository, EventConsumptionPlanRepository consumptionPlanRepository, CostCategoryRepository costCategoryRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.eventCostRepository = eventCostRepository;
        this.consumptionPlanRepository = consumptionPlanRepository;
        this.costCategoryRepository = costCategoryRepository;
    }

    public List<EventResponse> list() {
        Long companyId = SecurityUtils.currentCompanyId();
        return eventRepository.findByCompanyIdAndIsDeletedFalseOrderByEventDateAsc(companyId).stream().map(this::toResponse).toList();
    }

    public EventResponse get(Long id) {
        return toResponse(findEvent(id));
    }

    @Transactional
    public EventResponse create(EventUpsertRequest request) {
        validateDeposit(request);
        Event event = new Event();
        apply(event, request);
        event.setCompanyId(SecurityUtils.currentCompanyId());
        event.setStatus(EventStatus.PLANNING);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse update(Long id, EventUpsertRequest request) {
        validateDeposit(request);
        Event event = findEvent(id);
        apply(event, request);
        markWorkflowDirtyFromCosts(event);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse updateStatus(Long id, EventStatus status) {
        Event event = findEvent(id);
        event.setStatus(status);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse finalizeCosts(Long id) {
        Event event = findEvent(id);
        BigDecimal total = MathUtils.money(eventCostRepository.sumEstimatedCostByEvent(event.getCompanyId(), id));
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Finalize etmek için en az bir maliyet kalemi girilmelidir");
        }
        event.setCostFinalized(true);
        event.setPricingFinalized(false);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse finalizeConsumption(Long id) {
        Event event = findEvent(id);
        BigDecimal total = MathUtils.money(consumptionPlanRepository.sumByEvent(event.getCompanyId(), id));
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Finalize etmek için en az bir tüketim planı girilmelidir");
        }
        syncConsumptionCosts(event);
        event.setConsumptionFinalized(true);
        event.setCostFinalized(false);
        event.setPricingFinalized(false);
        return toResponse(eventRepository.save(event));
    }

    @Transactional
    public void markCostsDirty(Long eventId) {
        Event event = findEvent(eventId);
        event.setCostFinalized(false);
        event.setPricingFinalized(false);
        eventRepository.save(event);
    }

    @Transactional
    public void markConsumptionDirty(Long eventId) {
        Event event = findEvent(eventId);
        removeConsumptionCost(event);
        event.setConsumptionFinalized(false);
        event.setCostFinalized(false);
        event.setPricingFinalized(false);
        eventRepository.save(event);
    }

    public void validateConsumptionAllowed(Event event) {
        // Consumption planning is an input to costs, so it must be available before cost finalization.
    }

    public void validatePricingAllowed(Event event) {
        if (!Boolean.TRUE.equals(event.getConsumptionFinalized()) || !Boolean.TRUE.equals(event.getCostFinalized())) {
            throw new BadRequestException("Nihai fiyat için önce tüketim planını ve maliyetleri finalize edin");
        }
    }

    public void validateReservationReady(Event event) {
        if (!reservationReady(event)) {
            throw new BadRequestException("Rezervasyon almak için maliyet, tüketim planı ve nihai bilet fiyatı finalize edilmelidir");
        }
    }

    @Transactional
    public void delete(Long id) {
        Event event = findEvent(id);
        event.setIsDeleted(true);
        eventRepository.save(event);
    }

    public EventSummaryResponse summary(Long eventId) {
        Event event = findEvent(eventId);
        Long companyId = event.getCompanyId();

        long totalReservationCount = reservationRepository.countByCompanyIdAndEventId(companyId, eventId);
        long activeReservationCount = reservationRepository.countByCompanyIdAndEventIdAndReservationStatus(companyId, eventId, ReservationStatus.ACTIVE);
        long confirmedGuestCount = reservationRepository.sumGuestCountByCompanyAndEventAndStatus(companyId, eventId, ReservationStatus.ACTIVE, DepositStatus.PAID);
        long pendingDepositCount = reservationRepository.countByCompanyIdAndEventIdAndReservationStatusAndDepositStatus(companyId, eventId, ReservationStatus.ACTIVE, DepositStatus.PENDING);
        long paidDepositCount = reservationRepository.countByCompanyIdAndEventIdAndReservationStatusAndDepositStatus(companyId, eventId, ReservationStatus.ACTIVE, DepositStatus.PAID);

        BigDecimal totalDepositAmount = MathUtils.money(paymentRepository.sumByCompanyAndEventId(companyId, eventId));
        BigDecimal estimatedTicketRevenue = MathUtils.money(Optional.ofNullable(event.getFinalTicketPrice()).orElse(BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(event.getExpectedGuestCount())));
        BigDecimal estimatedTotalCost = MathUtils.money(eventCostRepository.sumEstimatedCostByEvent(companyId, eventId));
        BigDecimal estimatedProfit = MathUtils.money(estimatedTicketRevenue.subtract(estimatedTotalCost));
        BigDecimal estimatedProfitMargin = MathUtils.percentage(estimatedProfit, estimatedTicketRevenue);
        BigDecimal totalConsumptionPlanCost = MathUtils.money(consumptionPlanRepository.sumByEvent(companyId, eventId));

        List<EventCost> costs = eventCostRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(companyId, eventId);
        Map<Long, String> categoryNameMap = costCategoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(CostCategory::getId, CostCategory::getName));

        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (EventCost cost : costs) {
            String category = categoryNameMap.getOrDefault(cost.getCostCategoryId(), "Bilinmeyen");
            grouped.put(category, grouped.getOrDefault(category, BigDecimal.ZERO).add(Optional.ofNullable(cost.getTotalCost()).orElse(BigDecimal.ZERO)));
        }

        List<EventSummaryResponse.CategoryCostBreakdown> breakdown = grouped.entrySet().stream()
                .map(e -> new EventSummaryResponse.CategoryCostBreakdown(e.getKey(), MathUtils.money(e.getValue())))
                .toList();

        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getEventDate(),
                event.getVenueName(),
                event.getStatus(),
                event.getMaxCapacity(),
                event.getExpectedGuestCount(),
                MathUtils.money(event.getFinalTicketPrice()),
                MathUtils.money(event.getSuggestedTicketPrice()),
                event.getCostFinalized(),
                event.getConsumptionFinalized(),
                event.getPricingFinalized(),
                reservationReady(event),
                totalReservationCount,
                activeReservationCount,
                confirmedGuestCount,
                pendingDepositCount,
                paidDepositCount,
                totalDepositAmount,
                estimatedTicketRevenue,
                estimatedTotalCost,
                estimatedProfit,
                estimatedProfitMargin,
                totalConsumptionPlanCost,
                breakdown
        );
    }

    public Event findEvent(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        return eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void apply(Event event, EventUpsertRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setVenueName(request.venueName().trim());
        event.setMaxCapacity(request.maxCapacity());
        event.setExpectedGuestCount(request.expectedGuestCount());
        event.setTargetProfitMargin(MathUtils.money(request.targetProfitMargin()));
        event.setRevenueModel(request.revenueModel());
        event.setPrimaryContactId(request.primaryContactId());
        event.setAgreedRevenue(MathUtils.money(request.agreedRevenue()));
        event.setTicketPrice(null);
        event.setTargetTicketCount(request.expectedGuestCount());
        event.setComplimentaryGuestCount(request.complimentaryGuestCount() == null ? 0 : request.complimentaryGuestCount());
        event.setSponsorRevenueTarget(MathUtils.money(request.sponsorRevenueTarget()));
        event.setTargetRevenueAmount(resolveTargetRevenue(request));
        event.setTargetProfitAmount(null);
        event.setDepositRequired(request.depositRequired());
        event.setMinimumDepositAmount(MathUtils.money(request.minimumDepositAmount()));
    }

    private void validateDeposit(EventUpsertRequest request) {
        if (Boolean.TRUE.equals(request.depositRequired())
                && (request.minimumDepositAmount() == null || request.minimumDepositAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BadRequestException("Minimum deposit amount is required when deposit is mandatory");
        }
    }

    private BigDecimal resolveTargetRevenue(EventUpsertRequest request) {
        if (request.targetRevenueAmount() != null && request.targetRevenueAmount().compareTo(BigDecimal.ZERO) > 0) {
            return MathUtils.money(request.targetRevenueAmount());
        }
        BigDecimal agreed = Optional.ofNullable(request.agreedRevenue()).orElse(BigDecimal.ZERO);
        BigDecimal sponsor = Optional.ofNullable(request.sponsorRevenueTarget()).orElse(BigDecimal.ZERO);
        if (request.revenueModel() == EventRevenueModel.CLOSED_ORGANIZATION) {
            return MathUtils.money(agreed);
        }
        if (request.revenueModel() == EventRevenueModel.TICKETED_EVENT) {
            return MathUtils.money(sponsor);
        }
        return MathUtils.money(agreed.add(sponsor));
    }

    private void markWorkflowDirtyFromCosts(Event event) {
        event.setCostFinalized(false);
        event.setConsumptionFinalized(false);
        event.setPricingFinalized(false);
    }

    private boolean reservationReady(Event event) {
        return Boolean.TRUE.equals(event.getCostFinalized())
                && Boolean.TRUE.equals(event.getConsumptionFinalized())
                && Boolean.TRUE.equals(event.getPricingFinalized())
                && event.getFinalTicketPrice() != null
                && event.getFinalTicketPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private void syncConsumptionCosts(Event event) {
        removeConsumptionCost(event);
        List<EventConsumptionPlan> plans = consumptionPlanRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), event.getId());
        for (EventConsumptionPlan plan : plans) {
            EventCost cost = new EventCost();
            cost.setCompanyId(event.getCompanyId());
            cost.setEventId(event.getId());
            cost.setCostCategoryId(resolveConsumptionCategoryId(event.getCompanyId(), plan.getCategory()));
            cost.setName(CONSUMPTION_COST_PREFIX + plan.getProductName());
            cost.setDescription("Tüketim planından otomatik aktarıldı. Gerekli miktar: " + plan.getRequiredQuantity());
            cost.setCalculationType(CalculationType.PER_UNIT);
            cost.setUnitCost(MathUtils.money(plan.getUnitCost()));
            cost.setQuantity(plan.getRequiredQuantity().setScale(2, java.math.RoundingMode.HALF_UP));
            cost.setTotalCost(MathUtils.money(plan.getTotalCost()));
            cost.setIsEstimated(true);
            eventCostRepository.save(cost);
        }
    }

    private void removeConsumptionCost(Event event) {
        eventCostRepository
                .findByCompanyIdAndEventIdAndName(event.getCompanyId(), event.getId(), CONSUMPTION_COST_NAME)
                .ifPresent(eventCostRepository::delete);
        eventCostRepository
                .findByCompanyIdAndEventIdAndNameStartingWith(event.getCompanyId(), event.getId(), CONSUMPTION_COST_PREFIX)
                .forEach(eventCostRepository::delete);
    }

    private Long resolveConsumptionCategoryId(Long companyId, String categoryName) {
        return costCategoryRepository.findByCompanyIdAndNameIgnoreCase(companyId, categoryName)
                .or(() -> costCategoryRepository.findByCompanyIdAndNameIgnoreCase(companyId, CONSUMPTION_CATEGORY_NAME))
                .or(() -> costCategoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Cost category not found"))
                .getId();
    }

    private EventResponse toResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getVenueName(),
                event.getMaxCapacity(),
                event.getExpectedGuestCount(),
                MathUtils.money(event.getTargetProfitMargin()),
                event.getRevenueModel(),
                event.getPrimaryContactId(),
                MathUtils.money(event.getAgreedRevenue()),
                MathUtils.money(event.getTicketPrice()),
                event.getTargetTicketCount(),
                event.getComplimentaryGuestCount(),
                MathUtils.money(event.getSponsorRevenueTarget()),
                MathUtils.money(event.getTargetRevenueAmount()),
                MathUtils.money(event.getTargetProfitAmount()),
                MathUtils.money(event.getSuggestedTicketPrice()),
                MathUtils.money(event.getFinalTicketPrice()),
                event.getDepositRequired(),
                MathUtils.money(event.getMinimumDepositAmount()),
                event.getStatus(),
                event.getCostFinalized(),
                event.getConsumptionFinalized(),
                event.getPricingFinalized(),
                reservationReady(event),
                event.getCreatedAt()
        );
    }
}
