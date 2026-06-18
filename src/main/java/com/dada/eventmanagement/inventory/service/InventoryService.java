package com.dada.eventmanagement.inventory.service;

import com.dada.eventmanagement.common.enums.InventoryMovementType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.inventory.dto.*;
import com.dada.eventmanagement.inventory.entity.EventInventoryUsage;
import com.dada.eventmanagement.inventory.entity.InventoryItem;
import com.dada.eventmanagement.inventory.entity.InventoryMovement;
import com.dada.eventmanagement.inventory.repository.EventInventoryUsageRepository;
import com.dada.eventmanagement.inventory.repository.InventoryItemRepository;
import com.dada.eventmanagement.inventory.repository.InventoryMovementRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    private final InventoryItemRepository itemRepository;
    private final InventoryMovementRepository movementRepository;
    private final EventInventoryUsageRepository usageRepository;
    private final EventRepository eventRepository;
    private final FinanceService financeService;

    public InventoryService(
            InventoryItemRepository itemRepository,
            InventoryMovementRepository movementRepository,
            EventInventoryUsageRepository usageRepository,
            EventRepository eventRepository,
            FinanceService financeService
    ) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.usageRepository = usageRepository;
        this.eventRepository = eventRepository;
        this.financeService = financeService;
    }

    public List<InventoryItemResponse> items() {
        return itemRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(SecurityUtils.currentCompanyId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryItemResponse createItem(InventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setCompanyId(SecurityUtils.currentCompanyId());
        apply(item, request);
        item.setIsActive(true);
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse updateItem(Long id, InventoryItemRequest request) {
        InventoryItem item = findItem(id);
        apply(item, request);
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long id) {
        InventoryItem item = findItem(id);
        item.setIsActive(false);
        itemRepository.save(item);
    }

    public List<InventoryMovementResponse> movements(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        return mapMovements(movementRepository.findByCompanyIdAndMovementDateBetweenOrderByMovementDateDescIdDesc(companyId, start, end));
    }

    @Transactional
    public InventoryMovementResponse createMovement(InventoryMovementRequest request) {
        InventoryMovement movement = createMovementEntity(request);
        return mapMovements(List.of(movement)).get(0);
    }

    @Transactional
    public EventInventoryUsageResponse createUsage(EventInventoryUsageRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(request.eventId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        InventoryItem item = findItem(request.inventoryItemId());
        BigDecimal consumption = quantity(request.consumptionQuantity());
        BigDecimal waste = quantity(request.wasteQuantity());
        BigDecimal totalQuantity = consumption.add(waste).setScale(4, RoundingMode.HALF_UP);
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Consumption or waste quantity must be greater than zero");
        }

        BigDecimal unitCost = MathUtils.money(item.getUnitCost());
        BigDecimal totalCost = MathUtils.money(totalQuantity.multiply(unitCost));
        if (totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Usage cost must be greater than zero for finance tracking");
        }
        InventoryMovement movement = createMovementEntity(new InventoryMovementRequest(
                item.getId(),
                request.eventId(),
                InventoryMovementType.OUT,
                request.usageDate(),
                totalQuantity,
                unitCost,
                usageMovementNotes(request.notes())
        ));
        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                request.eventId(),
                request.accountId(),
                request.paymentMethodId(),
                request.categoryId(),
                null,
                null,
                FinancialTransactionType.EXPENSE,
                request.usageDate(),
                totalCost,
                request.actualGuestCount(),
                usageFinanceDescription(item, request)
        ));

        EventInventoryUsage usage = new EventInventoryUsage();
        usage.setCompanyId(companyId);
        usage.setEventId(request.eventId());
        usage.setInventoryItemId(item.getId());
        usage.setUsageDate(request.usageDate());
        usage.setActualGuestCount(request.actualGuestCount());
        usage.setConsumptionQuantity(consumption);
        usage.setWasteQuantity(waste);
        usage.setUnitCost(unitCost);
        usage.setTotalConsumptionCost(MathUtils.money(consumption.multiply(unitCost)));
        usage.setTotalWasteCost(MathUtils.money(waste.multiply(unitCost)));
        usage.setConsumptionPerPerson(perPerson(consumption, request.actualGuestCount()));
        usage.setWastePerPerson(perPerson(waste, request.actualGuestCount()));
        usage.setNotes(request.notes());
        usage.setInventoryMovementId(movement.getId());
        usage.setFinancialTransactionId(tx.getId());
        usage.setCreatedByUserId(SecurityUtils.currentUser().getId());
        return toResponse(usageRepository.save(usage), item);
    }

    public List<EventInventoryUsageResponse> eventUsages(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        Map<Long, InventoryItem> items = itemMap(companyId);
        return usageRepository.findByCompanyIdAndEventIdOrderByUsageDateDescIdDesc(companyId, eventId).stream()
                .map(row -> toResponse(row, items.get(row.getInventoryItemId())))
                .toList();
    }

    public List<EventInventoryUsageResponse> usageReport(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        Map<Long, InventoryItem> items = itemMap(companyId);
        return usageRepository.findByCompanyIdAndUsageDateBetweenOrderByUsageDateDescIdDesc(companyId, start, end).stream()
                .map(row -> toResponse(row, items.get(row.getInventoryItemId())))
                .toList();
    }

    private InventoryMovement createMovementEntity(InventoryMovementRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        InventoryItem item = findItem(request.inventoryItemId());
        if (request.eventId() != null) {
            eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(request.eventId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        }
        BigDecimal quantity = quantity(request.quantity());
        BigDecimal unitCost = MathUtils.money(request.unitCost() == null ? item.getUnitCost() : request.unitCost());
        applyStockChange(item, request.movementType(), quantity);
        itemRepository.save(item);

        InventoryMovement movement = new InventoryMovement();
        movement.setCompanyId(companyId);
        movement.setInventoryItemId(item.getId());
        movement.setEventId(request.eventId());
        movement.setMovementType(request.movementType());
        movement.setMovementDate(request.movementDate());
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        movement.setTotalAmount(MathUtils.money(quantity.multiply(unitCost)));
        movement.setNotes(request.notes());
        movement.setCreatedByUserId(SecurityUtils.currentUser().getId());
        return movementRepository.save(movement);
    }

    private void applyStockChange(InventoryItem item, InventoryMovementType type, BigDecimal quantity) {
        if (type == InventoryMovementType.IN) {
            item.setCurrentQuantity(item.getCurrentQuantity().add(quantity).setScale(4, RoundingMode.HALF_UP));
            return;
        }
        if (type == InventoryMovementType.OUT) {
            BigDecimal next = item.getCurrentQuantity().subtract(quantity).setScale(4, RoundingMode.HALF_UP);
            if (next.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Stock quantity cannot be negative");
            }
            item.setCurrentQuantity(next);
            return;
        }
        item.setCurrentQuantity(quantity);
    }

    private InventoryItem findItem(Long id) {
        return itemRepository.findByIdAndCompanyIdAndIsActiveTrue(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
    }

    private void apply(InventoryItem item, InventoryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category());
        item.setUnit(request.unit());
        item.setUnitCost(MathUtils.money(request.unitCost()));
        item.setCurrentQuantity(quantity(request.currentQuantity()));
        item.setMinimumQuantity(quantity(request.minimumQuantity()));
        item.setBottleVolumeMl(request.bottleVolumeMl() == null ? null : quantity(request.bottleVolumeMl()));
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getUnit(),
                MathUtils.money(item.getUnitCost()),
                quantity(item.getCurrentQuantity()),
                quantity(item.getMinimumQuantity()),
                item.getBottleVolumeMl() == null ? null : quantity(item.getBottleVolumeMl()),
                item.getCurrentQuantity().compareTo(item.getMinimumQuantity()) < 0
        );
    }

    private List<InventoryMovementResponse> mapMovements(List<InventoryMovement> rows) {
        Map<Long, InventoryItem> items = itemMap(SecurityUtils.currentCompanyId());
        return rows.stream().map(row -> {
            InventoryItem item = items.get(row.getInventoryItemId());
            return new InventoryMovementResponse(
                    row.getId(),
                    row.getInventoryItemId(),
                    item == null ? null : item.getName(),
                    item == null ? null : item.getUnit(),
                    row.getEventId(),
                    row.getMovementType(),
                    row.getMovementDate(),
                    quantity(row.getQuantity()),
                    MathUtils.money(row.getUnitCost()),
                    MathUtils.money(row.getTotalAmount()),
                    row.getNotes()
            );
        }).toList();
    }

    private EventInventoryUsageResponse toResponse(EventInventoryUsage usage, InventoryItem item) {
        return new EventInventoryUsageResponse(
                usage.getId(),
                usage.getEventId(),
                usage.getInventoryItemId(),
                item == null ? null : item.getName(),
                item == null ? null : item.getCategory(),
                item == null ? null : item.getUnit(),
                usage.getUsageDate(),
                usage.getActualGuestCount(),
                quantity(usage.getConsumptionQuantity()),
                quantity(usage.getWasteQuantity()),
                MathUtils.money(usage.getUnitCost()),
                MathUtils.money(usage.getTotalConsumptionCost()),
                MathUtils.money(usage.getTotalWasteCost()),
                quantity(usage.getConsumptionPerPerson()),
                quantity(usage.getWastePerPerson()),
                usage.getFinancialTransactionId(),
                usage.getNotes()
        );
    }

    private Map<Long, InventoryItem> itemMap(Long companyId) {
        return itemRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(InventoryItem::getId, Function.identity()));
    }

    private BigDecimal quantity(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal perPerson(BigDecimal quantity, Integer guestCount) {
        if (guestCount == null || guestCount <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return quantity.divide(BigDecimal.valueOf(guestCount), 4, RoundingMode.HALF_UP);
    }

    private String usageMovementNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Etkinlik tuketim/fire cikisi";
        }
        return "Etkinlik tuketim/fire cikisi - " + notes.trim();
    }

    private String usageFinanceDescription(InventoryItem item, EventInventoryUsageRequest request) {
        String base = "Stok tüketim/fire gideri - " + item.getName();
        if (request.notes() == null || request.notes().isBlank()) {
            return base;
        }
        return base + " - " + request.notes().trim();
    }
}
