package com.dada.eventmanagement.event.service;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.service.ContactService;
import com.dada.eventmanagement.event.dto.EventRevenueRequest;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.FinancialTransactionResponse;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
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
    public FinancialTransactionResponse create(Long eventId, EventRevenueRequest request) {
        Event event = eventService.findEvent(eventId);
        Long contactId = request.contactId() != null ? request.contactId() : event.getPrimaryContactId();
        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                event.getId(),
                request.accountId(),
                request.paymentMethodId(),
                request.categoryId(),
                request.revenueChannelId(),
                contactId,
                FinancialTransactionType.INCOME,
                request.transactionDate(),
                request.amount(),
                request.guestCount(),
                buildDescription(request),
                OperationSource.EVENT_REVENUE,
                OperationContext.EVENT,
                null
        ));
        if (contactId != null) {
            contactService.createMovement(contactId, new ContactMovementRequest(
                    request.transactionDate(),
                    ContactMovementType.COLLECTION,
                    request.amount(),
                    tx.getId(),
                    request.accountId(),
                    request.paymentMethodId(),
                    request.categoryId(),
                    request.revenueChannelId(),
                    event.getId(),
                    buildDescription(request)
            ));
        }
        return financeService.toTransactionResponse(tx);
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
