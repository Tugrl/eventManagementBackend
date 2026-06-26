package com.dada.eventmanagement.event.service;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import com.dada.eventmanagement.common.enums.EventRevenueCollectionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.dto.ContactMovementResponse;
import com.dada.eventmanagement.contact.service.ContactService;
import com.dada.eventmanagement.event.dto.EventRevenueRequest;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.FinancialTransactionResponse;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventRevenueService {
    private final EventService eventService;
    private final FinanceService financeService;
    private final ContactService contactService;

    public EventRevenueService(EventService eventService, FinanceService financeService, ContactService contactService) {
        this.eventService = eventService;
        this.financeService = financeService;
        this.contactService = contactService;
    }

    public List<FinancialTransactionResponse> list(Long eventId) {
        eventService.findEvent(eventId);
        return financeService.eventIncomeTransactions(eventId);
    }

    @Transactional
    public Object create(Long eventId, EventRevenueRequest request) {
        Event event = eventService.findEvent(eventId);
        EventRevenueCollectionStatus collectionStatus = request.collectionStatus() == null
                ? EventRevenueCollectionStatus.COLLECTED
                : request.collectionStatus();
        Long resolvedContactId = request.contactId() != null ? request.contactId() : event.getPrimaryContactId();

        if (collectionStatus == EventRevenueCollectionStatus.UNCOLLECTED) {
            if (request.contactId() == null) {
                throw new BadRequestException("Contact is required for uncollected event revenue");
            }
            ContactMovementResponse movement = contactService.createMovement(request.contactId(), new ContactMovementRequest(
                    request.transactionDate(),
                    ContactMovementType.RECEIVABLE,
                    request.amount(),
                    null,
                    null,
                    null,
                    request.categoryId(),
                    request.revenueChannelId(),
                    event.getId(),
                    null,
                    null,
                    buildDescription(request)
            ));
            return movement;
        }

        if (collectionStatus == EventRevenueCollectionStatus.PARTIALLY_COLLECTED) {
            if (resolvedContactId == null) {
                throw new BadRequestException("Contact is required for partially collected event revenue");
            }
            BigDecimal collectedAmount = request.collectedAmount();
            if (collectedAmount == null || collectedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Collected amount is required for partially collected event revenue");
            }
            if (collectedAmount.compareTo(request.amount()) >= 0) {
                throw new BadRequestException("Collected amount must be less than total amount for partially collected revenue");
            }
            ContactMovementResponse receivable = contactService.createMovement(resolvedContactId, new ContactMovementRequest(
                    request.transactionDate(),
                    ContactMovementType.RECEIVABLE,
                    request.amount(),
                    null,
                    null,
                    null,
                    request.categoryId(),
                    request.revenueChannelId(),
                    event.getId(),
                    null,
                    null,
                    buildDescription(request)
            ));
            ContactMovementResponse settlementMovement = contactService.createMovement(resolvedContactId, new ContactMovementRequest(
                    request.transactionDate(),
                    ContactMovementType.COLLECTION,
                    collectedAmount,
                    null,
                    request.accountId(),
                    request.paymentMethodId(),
                    request.categoryId(),
                    request.revenueChannelId(),
                    event.getId(),
                    receivable.documentId(),
                    null,
                    buildDescription(request)
            ));
            FinancialTransaction transaction = financeService.findTransaction(settlementMovement.financialTransactionId(), event.getCompanyId());
            return financeService.toTransactionResponse(transaction);
        }

        if (resolvedContactId == null) {
            FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                    event.getId(),
                    request.accountId(),
                    request.paymentMethodId(),
                    request.categoryId(),
                    request.revenueChannelId(),
                    null,
                    FinancialTransactionType.INCOME,
                    request.transactionDate(),
                    request.amount(),
                    request.guestCount(),
                    buildDescription(request),
                    OperationSource.EVENT_REVENUE,
                    OperationContext.EVENT,
                    null
            ));
            return financeService.toTransactionResponse(tx);
        }

        ContactMovementResponse receivable = contactService.createMovement(resolvedContactId, new ContactMovementRequest(
                request.transactionDate(),
                ContactMovementType.RECEIVABLE,
                request.amount(),
                null,
                null,
                null,
                request.categoryId(),
                request.revenueChannelId(),
                event.getId(),
                null,
                null,
                buildDescription(request)
        ));
        ContactMovementResponse settlementMovement = contactService.createMovement(resolvedContactId, new ContactMovementRequest(
                request.transactionDate(),
                ContactMovementType.COLLECTION,
                request.amount(),
                null,
                request.accountId(),
                request.paymentMethodId(),
                request.categoryId(),
                request.revenueChannelId(),
                event.getId(),
                receivable.documentId(),
                null,
                buildDescription(request)
        ));
        FinancialTransaction transaction = financeService.findTransaction(settlementMovement.financialTransactionId(), event.getCompanyId());
        return financeService.toTransactionResponse(transaction);
    }

    private String buildDescription(EventRevenueRequest request) {
        String prefix = switch (request.revenueType()) {
            case CUSTOMER_PAYMENT -> "Musteri tahsilati";
            case TICKET_SALE -> "Bilet satisi";
            case DOOR_SALE -> "Kapi satisi";
            case PLATFORM_SETTLEMENT -> "Platform tahsilati";
            case SPONSORSHIP -> "Sponsor geliri";
            case OTHER -> "Etkinlik geliri";
        };
        if (request.description() == null || request.description().isBlank()) {
            return prefix;
        }
        return prefix + " - " + request.description().trim();
    }
}
