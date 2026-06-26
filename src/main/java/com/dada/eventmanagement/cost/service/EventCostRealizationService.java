package com.dada.eventmanagement.cost.service;

import com.dada.eventmanagement.common.enums.*;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.consumption.repository.EventConsumptionPlanRepository;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.dto.ContactMovementResponse;
import com.dada.eventmanagement.contact.service.ContactService;
import com.dada.eventmanagement.cost.dto.*;
import com.dada.eventmanagement.cost.entity.EventCost;
import com.dada.eventmanagement.cost.entity.EventCostRealization;
import com.dada.eventmanagement.cost.repository.EventCostRealizationRepository;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.inventory.repository.EventInventoryReconciliationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventCostRealizationService {
    private final EventCostRealizationRepository repository;
    private final EventCostRepository eventCostRepository;
    private final EventService eventService;
    private final FinanceService financeService;
    private final ContactService contactService;
    private final EventConsumptionPlanRepository consumptionPlanRepository;
    private final EventInventoryReconciliationRepository inventoryReconciliationRepository;

    public EventCostRealizationService(EventCostRealizationRepository repository, EventCostRepository eventCostRepository, EventService eventService, FinanceService financeService, ContactService contactService, EventConsumptionPlanRepository consumptionPlanRepository, EventInventoryReconciliationRepository inventoryReconciliationRepository) {
        this.repository = repository;
        this.eventCostRepository = eventCostRepository;
        this.eventService = eventService;
        this.financeService = financeService;
        this.contactService = contactService;
        this.consumptionPlanRepository = consumptionPlanRepository;
        this.inventoryReconciliationRepository = inventoryReconciliationRepository;
    }

    @Transactional
    public List<EventCostRealizationResponse> list(Long eventId) {
        Event event = eventService.findEvent(eventId);
        initializePlannedCosts(event);
        return repository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), eventId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventCostRealizationResponse create(Long eventId, EventCostRealizationCreateRequest request) {
        Event event = eventService.findEvent(eventId);
        EventCostRealization row = new EventCostRealization();
        row.setCompanyId(event.getCompanyId());
        row.setEventId(eventId);
        row.setCostCategoryId(request.costCategoryId());
        row.setName(request.name().trim());
        row.setEstimatedUnitCost(BigDecimal.ZERO);
        row.setEstimatedQuantity(BigDecimal.ZERO);
        row.setEstimatedTotalCost(BigDecimal.ZERO);
        row.setActualUnitCost(MathUtils.money(request.actualUnitCost()));
        row.setActualQuantity(request.actualQuantity());
        row.setActualTotalCost(MathUtils.money(request.actualUnitCost().multiply(request.actualQuantity())));
        row.setVerificationStatus(CostVerificationStatus.ADJUSTED);
        row.setPaymentStatus(CostPaymentStatus.UNPAID);
        row.setNotes(request.notes());
        return toResponse(repository.save(row));
    }

    @Transactional
    public EventCostRealizationResponse update(Long eventId, Long id, EventCostRealizationRequest request) {
        Event event = eventService.findEvent(eventId);
        EventCostRealization row = find(event, id);
        if (Boolean.TRUE.equals(row.getFinalized())) {
            throw new BadRequestException("Finalized closing costs cannot be changed");
        }
        BigDecimal actualTotal = request.verificationStatus() == CostVerificationStatus.NOT_INCURRED
                ? BigDecimal.ZERO
                : MathUtils.money(request.actualUnitCost().multiply(request.actualQuantity()));
        row.setActualUnitCost(request.verificationStatus() == CostVerificationStatus.NOT_INCURRED ? BigDecimal.ZERO : MathUtils.money(request.actualUnitCost()));
        row.setActualQuantity(request.verificationStatus() == CostVerificationStatus.NOT_INCURRED ? BigDecimal.ZERO : request.actualQuantity());
        row.setActualTotalCost(actualTotal);
        row.setVerificationStatus(request.verificationStatus());
        row.setPaymentStatus(request.verificationStatus() == CostVerificationStatus.NOT_INCURRED ? CostPaymentStatus.UNPAID : request.paymentStatus());
        row.setAccountId(request.accountId());
        row.setPaymentMethodId(request.paymentMethodId());
        row.setFinancialCategoryId(request.financialCategoryId());
        row.setContactId(request.contactId());
        row.setTransactionDate(request.transactionDate());
        row.setNotes(request.notes());
        row.setVerifiedByUserId(SecurityUtils.currentUser().getId());
        row.setVerifiedAt(LocalDateTime.now());
        return toResponse(repository.save(row));
    }

    @Transactional
    public List<EventCostRealizationResponse> finalizeCosts(Long eventId) {
        Event event = eventService.findEvent(eventId);
        var inventoryPlans = consumptionPlanRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), eventId)
                .stream().filter(plan -> plan.getInventoryItemId() != null).toList();
        var inventoryRows = inventoryReconciliationRepository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), eventId);
        if (!inventoryPlans.isEmpty() && (inventoryRows.isEmpty() || inventoryRows.stream().anyMatch(row -> !Boolean.TRUE.equals(row.getFinalized())))) {
            throw new BadRequestException("Closing inventory must be finalized before closing costs");
        }
        initializePlannedCosts(event);
        List<EventCostRealization> rows = repository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), eventId);
        if (rows.isEmpty() || rows.stream().anyMatch(row -> row.getVerificationStatus() == CostVerificationStatus.PENDING)) {
            throw new BadRequestException("All closing cost items must be verified");
        }
        for (EventCostRealization row : rows) {
            if (row.getVerificationStatus() != CostVerificationStatus.NOT_INCURRED && row.getActualTotalCost().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Actual amount must be greater than zero for " + row.getName());
            }
            if (row.getPaymentStatus() == CostPaymentStatus.PAID && row.getVerificationStatus() != CostVerificationStatus.NOT_INCURRED) {
                postPaidCost(event, row);
            } else if (row.getContactId() != null && row.getVerificationStatus() != CostVerificationStatus.NOT_INCURRED && row.getContactMovementId() == null) {
                var movement = contactService.createMovement(row.getContactId(), new ContactMovementRequest(
                        row.getTransactionDate() == null ? event.getEventDate() : row.getTransactionDate(),
                        ContactMovementType.PAYABLE,
                        row.getActualTotalCost(),
                        null,
                        null,
                        null,
                        row.getFinancialCategoryId(),
                        null,
                        eventId,
                        null,
                        null,
                        "Etkinlik kapanis borcu - " + row.getName()
                ));
                row.setContactMovementId(movement.id());
                row.setFinanceDocumentId(movement.documentId());
            }
            row.setFinalized(true);
        }
        return repository.saveAll(rows).stream().map(this::toResponse).toList();
    }

    public BigDecimal totalFinalizedActualCost(Long eventId) {
        Event event = eventService.findEvent(eventId);
        return MathUtils.money(repository.sumFinalizedActualCost(event.getCompanyId(), eventId));
    }

    public void validateFinalized(Long eventId) {
        Event event = eventService.findEvent(eventId);
        List<EventCostRealization> rows = repository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), eventId);
        if (rows.isEmpty() || rows.stream().anyMatch(row -> !Boolean.TRUE.equals(row.getFinalized()))) {
            throw new BadRequestException("Event closing costs must be finalized before creating the report");
        }
    }

    private void postPaidCost(Event event, EventCostRealization row) {
        if (row.getFinancialTransactionId() != null || row.getSettlementId() != null) {
            return;
        }
        if (row.getContactId() == null) {
            if (row.getAccountId() == null || row.getFinancialCategoryId() == null || row.getTransactionDate() == null) {
                throw new BadRequestException("Paid cost requires account, financial category and transaction date: " + row.getName());
            }
            FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                    event.getId(), row.getAccountId(), row.getPaymentMethodId(), row.getFinancialCategoryId(),
                    null, row.getContactId(), FinancialTransactionType.EXPENSE, row.getTransactionDate(),
                    row.getActualTotalCost(), null, "Etkinlik kapanis gideri - " + row.getName(),
                    OperationSource.EVENT_CLOSING_COST, OperationContext.EVENT, row.getId()
            ));
            row.setFinancialTransactionId(tx.getId());
            return;
        }
        if (row.getFinancialCategoryId() == null || row.getTransactionDate() == null) {
            throw new BadRequestException("Paid cost requires financial category and transaction date: " + row.getName());
        }
        ContactMovementResponse payable = contactService.createMovement(row.getContactId(), new ContactMovementRequest(
                row.getTransactionDate(),
                ContactMovementType.PAYABLE,
                row.getActualTotalCost(),
                null,
                null,
                null,
                row.getFinancialCategoryId(),
                null,
                event.getId(),
                null,
                null,
                "Etkinlik kapanis gideri - " + row.getName()
        ));
        ContactMovementResponse payment = contactService.createMovement(row.getContactId(), new ContactMovementRequest(
                row.getTransactionDate(),
                ContactMovementType.PAYMENT,
                row.getActualTotalCost(),
                null,
                row.getAccountId(),
                row.getPaymentMethodId(),
                row.getFinancialCategoryId(),
                null,
                event.getId(),
                payable.documentId(),
                null,
                "Etkinlik kapanis gideri - " + row.getName()
        ));
        row.setContactMovementId(payment.id());
        row.setFinanceDocumentId(payable.documentId());
        row.setSettlementId(payment.settlementId());
        row.setFinancialTransactionId(payment.financialTransactionId());
    }

    private void initializePlannedCosts(Event event) {
        for (EventCost cost : eventCostRepository.findByCompanyIdAndEventIdOrderByCreatedAtDesc(event.getCompanyId(), event.getId())) {
            if (repository.findByCompanyIdAndEventIdAndEventCostId(event.getCompanyId(), event.getId(), cost.getId()).isPresent()) {
                continue;
            }
            EventCostRealization row = new EventCostRealization();
            row.setCompanyId(event.getCompanyId());
            row.setEventId(event.getId());
            row.setEventCostId(cost.getId());
            row.setCostCategoryId(cost.getCostCategoryId());
            row.setName(cost.getName());
            row.setEstimatedUnitCost(MathUtils.money(cost.getUnitCost()));
            row.setEstimatedQuantity(cost.getQuantity());
            row.setEstimatedTotalCost(MathUtils.money(cost.getTotalCost()));
            row.setActualUnitCost(MathUtils.money(cost.getUnitCost()));
            row.setActualQuantity(cost.getQuantity());
            row.setActualTotalCost(MathUtils.money(cost.getTotalCost()));
            if (cost.getFinancialTransactionId() != null) {
                row.setPaymentStatus(CostPaymentStatus.PAID);
                row.setFinancialTransactionId(cost.getFinancialTransactionId());
            }
            repository.save(row);
        }
    }

    private EventCostRealization find(Event event, Long id) {
        return repository.findByIdAndCompanyIdAndEventId(id, event.getCompanyId(), event.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Closing cost item not found"));
    }

    private EventCostRealizationResponse toResponse(EventCostRealization row) {
        return new EventCostRealizationResponse(
                row.getId(), row.getEventId(), row.getEventCostId(), row.getInventoryItemId(), row.getInventoryReconciliationId(), row.getCostCategoryId(), row.getName(),
                row.getEstimatedUnitCost(), row.getEstimatedQuantity(), row.getEstimatedTotalCost(),
                row.getActualUnitCost(), row.getActualQuantity(), row.getActualTotalCost(),
                row.getVerificationStatus(), row.getPaymentStatus(), row.getAccountId(), row.getPaymentMethodId(),
                row.getFinancialCategoryId(), row.getContactId(), row.getTransactionDate(),
                row.getFinancialTransactionId(), row.getContactMovementId(), row.getFinanceDocumentId(), row.getSettlementId(),
                row.getNotes(), row.getFinalized()
        );
    }
}
