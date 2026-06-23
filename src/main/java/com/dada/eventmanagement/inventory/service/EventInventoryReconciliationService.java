package com.dada.eventmanagement.inventory.service;

import com.dada.eventmanagement.common.enums.*;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.consumption.entity.EventConsumptionPlan;
import com.dada.eventmanagement.consumption.repository.EventConsumptionPlanRepository;
import com.dada.eventmanagement.cost.entity.EventCost;
import com.dada.eventmanagement.cost.entity.EventCostRealization;
import com.dada.eventmanagement.cost.repository.CostCategoryRepository;
import com.dada.eventmanagement.cost.repository.EventCostRealizationRepository;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.inventory.dto.*;
import com.dada.eventmanagement.inventory.entity.*;
import com.dada.eventmanagement.inventory.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventInventoryReconciliationService {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
    private final EventInventoryReconciliationRepository repository;
    private final EventConsumptionPlanRepository planRepository;
    private final EventInventoryUsageRepository usageRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;
    private final EventCostRealizationRepository costRealizationRepository;
    private final EventCostRepository eventCostRepository;
    private final CostCategoryRepository costCategoryRepository;
    private final EventService eventService;

    public EventInventoryReconciliationService(
            EventInventoryReconciliationRepository repository,
            EventConsumptionPlanRepository planRepository,
            EventInventoryUsageRepository usageRepository,
            InventoryItemRepository itemRepository,
            InventoryMovementRepository movementRepository,
            EventCostRealizationRepository costRealizationRepository,
            EventCostRepository eventCostRepository,
            CostCategoryRepository costCategoryRepository,
            EventService eventService
    ) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.usageRepository = usageRepository;
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.costRealizationRepository = costRealizationRepository;
        this.eventCostRepository = eventCostRepository;
        this.costCategoryRepository = costCategoryRepository;
        this.eventService = eventService;
    }

    @Transactional
    public List<EventInventoryReconciliationResponse> list(Long eventId) {
        Event event = eventService.findEvent(eventId);
        initialize(event);
        return rows(event).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventInventoryReconciliationResponse create(Long eventId, EventInventoryReconciliationCreateRequest request) {
        Event event = eventService.findEvent(eventId);
        if (repository.findByCompanyIdAndEventIdAndInventoryItemId(event.getCompanyId(), eventId, request.inventoryItemId()).isPresent()) {
            throw new BadRequestException("Inventory item is already in closing");
        }
        InventoryItem item = findItem(event, request.inventoryItemId());
        return toResponse(repository.save(newRow(event, item, null, BigDecimal.ZERO)));
    }

    @Transactional
    public EventInventoryReconciliationResponse update(Long eventId, Long id, EventInventoryReconciliationRequest request) {
        Event event = eventService.findEvent(eventId);
        EventInventoryReconciliation row = find(event, id);
        if (Boolean.TRUE.equals(row.getFinalized())) {
            throw new BadRequestException("Finalized inventory cannot be changed");
        }
        row.setInputUnit(request.inputUnit());
        row.setActualConsumptionInput(quantity(request.actualConsumption()));
        row.setManualWasteInput(quantity(request.manualWaste()));
        row.setActualGuestCount(request.actualGuestCount());
        row.setUsageDate(request.usageDate());
        row.setStatus(request.status());
        row.setNotes(request.notes());
        calculate(row, findItem(event, row.getInventoryItemId()));
        return toResponse(repository.save(row));
    }

    @Transactional
    public List<EventInventoryReconciliationResponse> finalizeInventory(Long eventId) {
        Event event = eventService.findEvent(eventId);
        initialize(event);
        List<EventInventoryReconciliation> rows = rows(event);
        if (rows.stream().anyMatch(row -> row.getStatus() != InventoryReconciliationStatus.VERIFIED)) {
            throw new BadRequestException("All closing inventory items must be verified");
        }
        for (EventInventoryReconciliation row : rows) {
            if (Boolean.TRUE.equals(row.getFinalized())) continue;
            InventoryItem item = findItem(event, row.getInventoryItemId());
            calculate(row, item);
            BigDecimal delta = row.getDeductedQuantity().subtract(row.getPreviouslyProcessedQuantity()).setScale(4, RoundingMode.HALF_UP);
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                InventoryMovementType type = delta.signum() > 0 ? InventoryMovementType.OUT : InventoryMovementType.IN;
                BigDecimal movementQuantity = delta.abs();
                applyStock(item, type, movementQuantity);
                itemRepository.save(item);
                InventoryMovement movement = new InventoryMovement();
                movement.setCompanyId(event.getCompanyId());
                movement.setInventoryItemId(item.getId());
                movement.setEventId(eventId);
                movement.setMovementType(type);
                movement.setMovementDate(row.getUsageDate());
                movement.setQuantity(movementQuantity);
                movement.setUnitCost(MathUtils.money(item.getUnitCost()));
                movement.setTotalAmount(MathUtils.money(movementQuantity.multiply(item.getUnitCost())));
                movement.setNotes("Etkinlik kapanış stok mutabakatı - " + item.getName());
                movement.setCreatedByUserId(SecurityUtils.currentUser().getId());
                row.setInventoryMovementId(movementRepository.save(movement).getId());
            }
            syncActualCost(event, item, row);
            row.setFinalized(true);
        }
        return repository.saveAll(rows).stream().map(this::toResponse).toList();
    }

    private void initialize(Event event) {
        List<EventConsumptionPlan> plans = planRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), event.getId());
        Map<Long, List<EventConsumptionPlan>> grouped = plans.stream()
                .filter(plan -> plan.getInventoryItemId() != null)
                .collect(Collectors.groupingBy(EventConsumptionPlan::getInventoryItemId));
        for (var entry : grouped.entrySet()) {
            if (repository.findByCompanyIdAndEventIdAndInventoryItemId(event.getCompanyId(), event.getId(), entry.getKey()).isPresent()) continue;
            InventoryItem item = findItem(event, entry.getKey());
            BigDecimal planned = entry.getValue().stream().map(EventConsumptionPlan::getRequiredQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            repository.save(newRow(event, item, entry.getValue().get(0).getId(), planned));
        }
    }

    private EventInventoryReconciliation newRow(Event event, InventoryItem item, Long planId, BigDecimal planned) {
        EventInventoryReconciliation row = new EventInventoryReconciliation();
        row.setCompanyId(event.getCompanyId());
        row.setEventId(event.getId());
        row.setInventoryItemId(item.getId());
        row.setConsumptionPlanId(planId);
        row.setItemName(item.getName());
        row.setStockUnit(item.getUnit());
        row.setPlannedQuantity(quantity(planned));
        row.setUnitCost(MathUtils.money(item.getUnitCost()));
        row.setActualGuestCount(event.getExpectedGuestCount());
        row.setUsageDate(event.getEventDate());
        BigDecimal processed = usageRepository.findByCompanyIdAndEventIdOrderByUsageDateDescIdDesc(event.getCompanyId(), event.getId()).stream()
                .filter(usage -> usage.getInventoryItemId().equals(item.getId()))
                .map(usage -> usage.getConsumptionQuantity().add(usage.getWasteQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        row.setPreviouslyProcessedQuantity(quantity(processed));
        BigDecimal initialStockQuantity = processed.compareTo(BigDecimal.ZERO) > 0 ? processed : planned;
        if (item.getUnit() == InventoryUnit.BOTTLE) {
            requireBottleVolume(item);
            row.setInputUnit(InventoryUnit.MILLILITER);
            row.setActualConsumptionInput(quantity(initialStockQuantity.multiply(item.getBottleVolumeMl())));
        } else {
            row.setInputUnit(item.getUnit());
            row.setActualConsumptionInput(quantity(initialStockQuantity));
        }
        row.setManualWasteInput(BigDecimal.ZERO);
        calculate(row, item);
        return row;
    }

    private void calculate(EventInventoryReconciliation row, InventoryItem item) {
        if (item.getUnit() == InventoryUnit.BOTTLE) {
            calculateBottle(row, item);
            return;
        }
        BigDecimal consumption = convertToStockUnit(item.getUnit(), row.getInputUnit(), row.getActualConsumptionInput());
        BigDecimal waste = convertToStockUnit(item.getUnit(), row.getInputUnit(), row.getManualWasteInput());
        row.setActualConsumptionStock(quantity(consumption));
        row.setTotalWasteStock(quantity(waste));
        row.setAutomaticWasteMl(BigDecimal.ZERO);
        row.setDeductedQuantity(quantity(consumption.add(waste)));
        row.setTotalCost(MathUtils.money(row.getDeductedQuantity().multiply(item.getUnitCost())));
    }

    private void calculateBottle(EventInventoryReconciliation row, InventoryItem item) {
        BigDecimal volume = requireBottleVolume(item);
        if (row.getInputUnit() == InventoryUnit.BOTTLE) {
            requireWholeBottle(row.getActualConsumptionInput());
            requireWholeBottle(row.getManualWasteInput());
            row.setActualConsumptionStock(quantity(row.getActualConsumptionInput()));
            row.setTotalWasteStock(quantity(row.getManualWasteInput()));
            row.setAutomaticWasteMl(BigDecimal.ZERO);
            row.setDeductedQuantity(quantity(row.getActualConsumptionInput().add(row.getManualWasteInput())));
        } else {
            BigDecimal consumptionMl = toMilliliters(row.getInputUnit(), row.getActualConsumptionInput());
            BigDecimal manualWasteMl = toMilliliters(row.getInputUnit(), row.getManualWasteInput());
            BigDecimal totalMl = consumptionMl.add(manualWasteMl);
            BigDecimal bottles = totalMl.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : totalMl.divide(volume, 0, RoundingMode.CEILING);
            BigDecimal autoWasteMl = bottles.multiply(volume).subtract(totalMl);
            row.setActualConsumptionStock(quantity(consumptionMl.divide(volume, 6, RoundingMode.HALF_UP)));
            row.setTotalWasteStock(quantity(manualWasteMl.add(autoWasteMl).divide(volume, 6, RoundingMode.HALF_UP)));
            row.setAutomaticWasteMl(quantity(autoWasteMl));
            row.setDeductedQuantity(quantity(bottles));
        }
        row.setTotalCost(MathUtils.money(row.getDeductedQuantity().multiply(item.getUnitCost())));
    }

    private void syncActualCost(Event event, InventoryItem item, EventInventoryReconciliation inventoryRow) {
        EventCostRealization costRow = costRealizationRepository
                .findByCompanyIdAndEventIdAndInventoryItemId(event.getCompanyId(), event.getId(), item.getId())
                .orElseGet(() -> findOrCreateCostRealization(event, item));
        if (Boolean.TRUE.equals(costRow.getFinalized())) {
            throw new BadRequestException("Inventory cost is already finalized: " + item.getName());
        }
        costRow.setInventoryItemId(item.getId());
        costRow.setInventoryReconciliationId(inventoryRow.getId());
        costRow.setActualUnitCost(MathUtils.money(item.getUnitCost()));
        costRow.setActualQuantity(inventoryRow.getDeductedQuantity());
        costRow.setActualTotalCost(inventoryRow.getTotalCost());
        costRow.setVerificationStatus(CostVerificationStatus.CONFIRMED);
        if (costRow.getFinancialTransactionId() == null) {
            costRow.setPaymentStatus(CostPaymentStatus.STOCK);
            costRow.setAccountId(null);
            costRow.setPaymentMethodId(null);
            costRow.setFinancialCategoryId(null);
            costRow.setContactId(null);
            costRow.setTransactionDate(null);
        } else {
            costRow.setPaymentStatus(CostPaymentStatus.PAID);
        }
        costRow.setNotes("Stok kapanışından otomatik hesaplandı");
        costRealizationRepository.save(costRow);
    }

    private EventCostRealization findOrCreateCostRealization(Event event, InventoryItem item) {
        Optional<EventCostRealization> byName = costRealizationRepository
                .findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), event.getId()).stream()
                .filter(row -> row.getName().endsWith(item.getName()))
                .findFirst();
        if (byName.isPresent()) return byName.get();
        Optional<EventCost> plannedCost = eventCostRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), event.getId())
                .stream().filter(row -> row.getName().endsWith(item.getName())).findFirst();
        Long categoryId = plannedCost.map(EventCost::getCostCategoryId)
                .orElseGet(() -> costCategoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(event.getCompanyId()).stream().findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Cost category not found")).getId());
        EventCostRealization row = new EventCostRealization();
        row.setCompanyId(event.getCompanyId());
        row.setEventId(event.getId());
        row.setEventCostId(plannedCost.map(EventCost::getId).orElse(null));
        row.setCostCategoryId(categoryId);
        row.setName("Stok kullanımı - " + item.getName());
        row.setEstimatedUnitCost(plannedCost.map(EventCost::getUnitCost).orElse(item.getUnitCost()));
        row.setEstimatedQuantity(plannedCost.map(EventCost::getQuantity).orElse(BigDecimal.ZERO));
        row.setEstimatedTotalCost(plannedCost.map(EventCost::getTotalCost).orElse(BigDecimal.ZERO));
        return row;
    }

    private BigDecimal convertToStockUnit(InventoryUnit stockUnit, InventoryUnit inputUnit, BigDecimal amount) {
        if (stockUnit == inputUnit) return amount;
        return switch (stockUnit) {
            case MILLILITER -> inputUnit == InventoryUnit.LITER ? amount.multiply(THOUSAND) : unsupported();
            case LITER -> inputUnit == InventoryUnit.MILLILITER ? amount.divide(THOUSAND, 6, RoundingMode.HALF_UP) : unsupported();
            case GRAM -> inputUnit == InventoryUnit.KILOGRAM ? amount.multiply(THOUSAND) : unsupported();
            case KILOGRAM -> inputUnit == InventoryUnit.GRAM ? amount.divide(THOUSAND, 6, RoundingMode.HALF_UP) : unsupported();
            case PIECE -> unsupported();
            case BOTTLE -> unsupported();
        };
    }

    private BigDecimal toMilliliters(InventoryUnit inputUnit, BigDecimal amount) {
        if (inputUnit == InventoryUnit.MILLILITER) return amount;
        if (inputUnit == InventoryUnit.LITER) return amount.multiply(THOUSAND);
        throw new BadRequestException("Bottle items support bottle, ml or liter input");
    }

    private BigDecimal unsupported() {
        throw new BadRequestException("Selected inventory unit conversion is not supported");
    }

    private void requireWholeBottle(BigDecimal value) {
        if (value.stripTrailingZeros().scale() > 0) {
            throw new BadRequestException("Bottle quantities must be whole numbers");
        }
    }

    private BigDecimal requireBottleVolume(InventoryItem item) {
        if (item.getBottleVolumeMl() == null || item.getBottleVolumeMl().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Bottle volume must be configured for " + item.getName());
        }
        return item.getBottleVolumeMl();
    }

    private void applyStock(InventoryItem item, InventoryMovementType type, BigDecimal amount) {
        if (type == InventoryMovementType.OUT) {
            BigDecimal next = item.getCurrentQuantity().subtract(amount).setScale(4, RoundingMode.HALF_UP);
            if (next.compareTo(BigDecimal.ZERO) < 0) throw new BadRequestException("Insufficient stock for " + item.getName());
            item.setCurrentQuantity(next);
        } else {
            item.setCurrentQuantity(item.getCurrentQuantity().add(amount).setScale(4, RoundingMode.HALF_UP));
        }
    }

    private InventoryItem findItem(Event event, Long itemId) {
        return itemRepository.findByIdAndCompanyIdAndIsActiveTrue(itemId, event.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private EventInventoryReconciliation find(Event event, Long id) {
        return repository.findByIdAndCompanyIdAndEventId(id, event.getCompanyId(), event.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Closing inventory item not found"));
    }

    private List<EventInventoryReconciliation> rows(Event event) {
        return repository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), event.getId());
    }

    private BigDecimal quantity(BigDecimal value) {
        return Optional.ofNullable(value).orElse(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private EventInventoryReconciliationResponse toResponse(EventInventoryReconciliation row) {
        return new EventInventoryReconciliationResponse(
                row.getId(), row.getEventId(), row.getInventoryItemId(), row.getItemName(), row.getStockUnit(), row.getInputUnit(),
                row.getPlannedQuantity(), row.getActualConsumptionInput(), row.getManualWasteInput(), row.getActualConsumptionStock(),
                row.getTotalWasteStock(), row.getAutomaticWasteMl(), row.getDeductedQuantity(), row.getPreviouslyProcessedQuantity(),
                row.getUnitCost(), row.getTotalCost(), row.getActualGuestCount(), row.getUsageDate(), row.getStatus(),
                row.getFinalized(), row.getInventoryMovementId(), row.getNotes()
        );
    }
}
