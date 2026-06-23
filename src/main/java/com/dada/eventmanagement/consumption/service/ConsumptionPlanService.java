package com.dada.eventmanagement.consumption.service;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.consumption.dto.ConsumptionPlanRequest;
import com.dada.eventmanagement.consumption.dto.ConsumptionPlanResponse;
import com.dada.eventmanagement.consumption.entity.EventConsumptionPlan;
import com.dada.eventmanagement.consumption.repository.EventConsumptionPlanRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.inventory.entity.InventoryItem;
import com.dada.eventmanagement.inventory.repository.InventoryItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsumptionPlanService {
    private final EventConsumptionPlanRepository repository;
    private final EventService eventService;
    private final InventoryItemRepository inventoryItemRepository;

    public ConsumptionPlanService(EventConsumptionPlanRepository repository, EventService eventService, InventoryItemRepository inventoryItemRepository) {
        this.repository = repository;
        this.eventService = eventService;
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public List<ConsumptionPlanResponse> list(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return repository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ConsumptionPlanResponse create(Long eventId, ConsumptionPlanRequest request) {
        Event event = eventService.findEvent(eventId);
        eventService.validateConsumptionAllowed(event);
        EventConsumptionPlan entity = new EventConsumptionPlan();
        entity.setCompanyId(event.getCompanyId());
        entity.setEventId(eventId);
        apply(entity, request);
        EventConsumptionPlan saved = repository.save(entity);
        eventService.markConsumptionDirty(eventId);
        return toResponse(saved);
    }

    @Transactional
    public ConsumptionPlanResponse update(Long id, ConsumptionPlanRequest request) {
        EventConsumptionPlan entity = find(id);
        eventService.validateConsumptionAllowed(eventService.findEvent(entity.getEventId()));
        apply(entity, request);
        EventConsumptionPlan saved = repository.save(entity);
        eventService.markConsumptionDirty(entity.getEventId());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        EventConsumptionPlan entity = find(id);
        Long eventId = entity.getEventId();
        repository.delete(entity);
        eventService.markConsumptionDirty(eventId);
    }

    public BigDecimal totalCost(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return MathUtils.money(repository.sumByEvent(event.getCompanyId(), eventId));
    }

    private EventConsumptionPlan find(Long id) {
        return repository.findByIdAndCompanyId(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Consumption plan not found"));
    }

    private void apply(EventConsumptionPlan entity, ConsumptionPlanRequest request) {
        InventoryItem item = inventoryItemRepository
                .findByIdAndCompanyIdAndIsActiveTrue(request.inventoryItemId(), SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found"));
        entity.setInventoryItemId(item.getId());
        entity.setProductName(item.getName());
        entity.setCategory(request.category().trim());
        entity.setEstimatedConsumptionPerPerson(request.estimatedConsumptionPerPerson().setScale(4, RoundingMode.HALF_UP));
        entity.setInputUnit(request.inputUnit());
        int expectedGuestCount = request.expectedGuestCount() == null || request.expectedGuestCount() <= 0
                ? eventService.findEvent(entity.getEventId()).getExpectedGuestCount()
                : request.expectedGuestCount();
        entity.setExpectedGuestCount(expectedGuestCount);
        entity.setWastePercentage(MathUtils.money(request.wastePercentage()));
        entity.setUnitCost(MathUtils.money(item.getUnitCost()));
        BigDecimal convertedPerPerson = ConsumptionUnitConverter.convertToStockUnit(item, request.inputUnit(), entity.getEstimatedConsumptionPerPerson());
        entity.setConvertedUnitQuantity(convertedPerPerson.setScale(4, RoundingMode.HALF_UP));

        BigDecimal wasteMultiplier = BigDecimal.ONE.add(entity.getWastePercentage().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal requiredQuantity = BigDecimal.valueOf(entity.getExpectedGuestCount())
                .multiply(entity.getConvertedUnitQuantity())
                .multiply(wasteMultiplier);
        entity.setRequiredQuantity(requiredQuantity.setScale(4, RoundingMode.HALF_UP));
        entity.setTotalCost(MathUtils.money(entity.getRequiredQuantity().multiply(entity.getUnitCost())));
        entity.setNotes(request.notes());
    }

    private ConsumptionPlanResponse toResponse(EventConsumptionPlan p) {
        return new ConsumptionPlanResponse(
                p.getId(),
                p.getEventId(),
                p.getInventoryItemId(),
                p.getProductName(),
                p.getCategory(),
                p.getEstimatedConsumptionPerPerson(),
                p.getInputUnit(),
                resolveStockUnit(p.getInventoryItemId()),
                p.getExpectedGuestCount(),
                MathUtils.money(p.getWastePercentage()),
                MathUtils.money(p.getUnitCost()),
                p.getConvertedUnitQuantity(),
                p.getRequiredQuantity(),
                MathUtils.money(p.getTotalCost()),
                p.getNotes(),
                p.getCreatedAt()
        );
    }

    private InventoryUnit resolveStockUnit(Long inventoryItemId) {
        if (inventoryItemId == null) {
            return null;
        }
        return inventoryItemRepository.findByIdAndCompanyIdAndIsActiveTrue(inventoryItemId, SecurityUtils.currentCompanyId())
                .map(InventoryItem::getUnit)
                .orElse(null);
    }
}
