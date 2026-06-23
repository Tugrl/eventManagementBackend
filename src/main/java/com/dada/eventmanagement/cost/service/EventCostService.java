package com.dada.eventmanagement.cost.service;

import com.dada.eventmanagement.common.enums.CalculationType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.cost.dto.CostSummaryResponse;
import com.dada.eventmanagement.cost.dto.EventCostRequest;
import com.dada.eventmanagement.cost.dto.EventCostResponse;
import com.dada.eventmanagement.cost.dto.PostEventCostToFinanceRequest;
import com.dada.eventmanagement.cost.entity.EventCost;
import com.dada.eventmanagement.cost.repository.CostCategoryRepository;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCostService {
    private final EventCostRepository repository;
    private final CostCategoryRepository categoryRepository;
    private final EventService eventService;
    private final FinanceService financeService;

    public EventCostService(
            EventCostRepository repository,
            CostCategoryRepository categoryRepository,
            EventService eventService,
            FinanceService financeService
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.eventService = eventService;
        this.financeService = financeService;
    }

    public List<EventCostResponse> list(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return repository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), eventId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventCostResponse create(Long eventId, EventCostRequest request) {
        Event event = eventService.findEvent(eventId);
        EventCost entity = new EventCost();
        entity.setCompanyId(event.getCompanyId());
        entity.setEventId(eventId);
        apply(entity, request, event);
        EventCost saved = repository.save(entity);
        eventService.markCostsDirty(eventId);
        return toResponse(saved);
    }

    @Transactional
    public EventCostResponse update(Long id, EventCostRequest request) {
        EventCost entity = find(id);
        ensureNotPosted(entity);
        Event event = eventService.findEvent(entity.getEventId());
        apply(entity, request, event);
        EventCost saved = repository.save(entity);
        eventService.markCostsDirty(entity.getEventId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        EventCost entity = find(id);
        ensureNotPosted(entity);
        Long eventId = entity.getEventId();
        repository.delete(entity);
        eventService.markCostsDirty(eventId);
    }

    @Transactional
    public EventCostResponse postToFinance(Long id, PostEventCostToFinanceRequest request) {
        EventCost entity = find(id);
        if (entity.getFinancialTransactionId() != null) {
            throw new BadRequestException("This cost is already posted to finance");
        }
        if (Boolean.TRUE.equals(entity.getIsEstimated())) {
            throw new BadRequestException("Estimated costs cannot be posted to finance");
        }
        Event event = eventService.findEvent(entity.getEventId());
        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                entity.getEventId(),
                request.accountId(),
                request.paymentMethodId(),
                request.categoryId(),
                null,
                null,
                FinancialTransactionType.EXPENSE,
                request.transactionDate(),
                MathUtils.money(entity.getTotalCost()),
                event.getExpectedGuestCount(),
                financeDescription(entity, request),
                OperationSource.COMPANY_EXPENSE,
                OperationContext.EVENT,
                entity.getId()
        ));
        entity.setFinancialTransactionId(tx.getId());
        return toResponse(repository.save(entity));
    }

    public CostSummaryResponse summary(Long eventId) {
        Event event = eventService.findEvent(eventId);
        Long companyId = event.getCompanyId();
        List<EventCost> costs = repository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(companyId, eventId);

        BigDecimal total = MathUtils.money(repository.sumEstimatedCostByEvent(companyId, eventId));
        BigDecimal fixed = MathUtils.money(repository.sumByType(companyId, eventId, CalculationType.FIXED));
        BigDecimal perPerson = MathUtils.money(repository.sumByType(companyId, eventId, CalculationType.PER_PERSON));
        BigDecimal staff = MathUtils.money(repository.sumByType(companyId, eventId, CalculationType.PER_STAFF));
        BigDecimal perUnit = MathUtils.money(repository.sumByType(companyId, eventId, CalculationType.PER_UNIT));
        BigDecimal manual = MathUtils.money(repository.sumByType(companyId, eventId, CalculationType.MANUAL));

        Map<Long, String> categoryMap = categoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId)
                .stream().collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
        Map<String, BigDecimal> grouped = new LinkedHashMap<>();
        for (EventCost c : costs) {
            String categoryName = categoryMap.getOrDefault(c.getCostCategoryId(), "Bilinmeyen");
            grouped.merge(categoryName, Optional.ofNullable(c.getTotalCost()).orElse(BigDecimal.ZERO), BigDecimal::add);
        }
        List<CostSummaryResponse.CategoryBreakdown> breakdown = grouped.entrySet().stream()
                .map(e -> new CostSummaryResponse.CategoryBreakdown(e.getKey(), MathUtils.money(e.getValue())))
                .toList();

        return new CostSummaryResponse(eventId, total, fixed, perPerson, staff, perUnit, manual, breakdown);
    }

    public BigDecimal totalEstimatedCost(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return MathUtils.money(repository.sumEstimatedCostByEvent(event.getCompanyId(), eventId));
    }

    public EventCost find(Long id) {
        return repository.findByIdAndCompanyId(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Event cost not found"));
    }

    private void apply(EventCost entity, EventCostRequest request, Event event) {
        categoryRepository.findByIdAndCompanyId(request.costCategoryId(), event.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Cost category not found"));
        entity.setCostCategoryId(request.costCategoryId());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setCalculationType(request.calculationType());
        entity.setUnitCost(MathUtils.money(request.unitCost()));
        entity.setQuantity(request.quantity().setScale(2, RoundingMode.HALF_UP));
        entity.setIsEstimated(request.isEstimated());
        entity.setTotalCost(calculateTotal(request, event));
    }

    private BigDecimal calculateTotal(EventCostRequest request, Event event) {
        return switch (request.calculationType()) {
            case FIXED, PER_STAFF, PER_UNIT ->
                    MathUtils.money(request.unitCost().multiply(request.quantity()));
            case PER_PERSON ->
                    MathUtils.money(request.unitCost().multiply(BigDecimal.valueOf(event.getExpectedGuestCount())));
            case MANUAL ->
                    MathUtils.money(Optional.ofNullable(request.totalCost()).orElse(BigDecimal.ZERO));
        };
    }

    private EventCostResponse toResponse(EventCost ec) {
        return new EventCostResponse(
                ec.getId(),
                ec.getEventId(),
                ec.getCostCategoryId(),
                ec.getName(),
                ec.getDescription(),
                ec.getCalculationType(),
                MathUtils.money(ec.getUnitCost()),
                ec.getQuantity(),
                MathUtils.money(ec.getTotalCost()),
                ec.getIsEstimated(),
                ec.getFinancialTransactionId(),
                ec.getCreatedAt()
        );
    }

    private void ensureNotPosted(EventCost entity) {
        if (entity.getFinancialTransactionId() != null) {
            throw new BadRequestException("Posted costs cannot be changed");
        }
    }

    private String financeDescription(EventCost entity, PostEventCostToFinanceRequest request) {
        if (request.description() != null && !request.description().isBlank()) {
            return request.description().trim();
        }
        return "Etkinlik maliyeti - " + entity.getName();
    }
}
