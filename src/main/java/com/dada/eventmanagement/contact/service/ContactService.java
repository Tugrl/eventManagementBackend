package com.dada.eventmanagement.contact.service;

import com.dada.eventmanagement.common.enums.ContactMovementType;
import com.dada.eventmanagement.common.enums.ContactType;
import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.dto.ContactMovementResponse;
import com.dada.eventmanagement.contact.dto.ContactRequest;
import com.dada.eventmanagement.contact.dto.ContactResponse;
import com.dada.eventmanagement.contact.entity.Contact;
import com.dada.eventmanagement.contact.entity.ContactMovement;
import com.dada.eventmanagement.contact.repository.ContactMovementRepository;
import com.dada.eventmanagement.contact.repository.ContactRepository;
import com.dada.eventmanagement.finance.dto.FinanceDocumentResponse;
import com.dada.eventmanagement.finance.dto.FinanceDocumentSettlementRequest;
import com.dada.eventmanagement.finance.dto.VoidTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinanceDocument;
import com.dada.eventmanagement.finance.entity.FinanceDocumentSettlement;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {
    private final ContactRepository contactRepository;
    private final ContactMovementRepository movementRepository;
    private final FinanceService financeService;

    public ContactService(ContactRepository contactRepository, ContactMovementRepository movementRepository, FinanceService financeService) {
        this.contactRepository = contactRepository;
        this.movementRepository = movementRepository;
        this.financeService = financeService;
    }

    public List<ContactResponse> list(ContactType type) {
        Long companyId = SecurityUtils.currentCompanyId();
        List<Contact> rows = type == null
                ? contactRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId)
                : contactRepository.findByCompanyIdAndContactTypeAndIsActiveTrueOrderByNameAsc(companyId, type);
        return mapContacts(rows);
    }

    @Transactional
    public ContactResponse create(ContactRequest request) {
        BigDecimal openingBalance = nvl(request.openingBalance());
        Contact contact = new Contact();
        contact.setCompanyId(SecurityUtils.currentCompanyId());
        contact.setName(request.name().trim());
        contact.setContactType(request.contactType());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setTaxNumber(request.taxNumber());
        contact.setNotes(request.notes());
        contact.setOpeningBalance(openingBalance);
        contact.setCurrentBalance(openingBalance);
        contact.setIsActive(true);
        contact = contactRepository.save(contact);
        return mapContacts(List.of(contact)).get(0);
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = find(id);
        BigDecimal newOpening = nvl(request.openingBalance());
        contact.setName(request.name().trim());
        contact.setContactType(request.contactType());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setTaxNumber(request.taxNumber());
        contact.setNotes(request.notes());
        contact.setOpeningBalance(newOpening);
        contact.setCurrentBalance(calculateCurrentBalance(contact, newOpening));
        contact = contactRepository.save(contact);
        return mapContacts(List.of(contact)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        Contact contact = find(id);
        contact.setIsActive(false);
        contactRepository.save(contact);
    }

    public List<ContactMovementResponse> movements(Long contactId) {
        Contact contact = find(contactId);
        return movementRepository.findByCompanyIdAndContactIdOrderByMovementDateDescIdDesc(contact.getCompanyId(), contact.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FinanceDocumentResponse> documents(
            Long contactId,
            FinanceDocumentType documentType,
            FinanceDocumentStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        find(contactId);
        return financeService.contactDocuments(contactId, documentType, status, startDate, endDate);
    }

    @Transactional
    public ContactMovementResponse createMovement(Long contactId, ContactMovementRequest request) {
        Contact contact = find(contactId);
        ContactMovement movement = switch (request.movementType()) {
            case RECEIVABLE -> createDocumentBackedMovement(contact, request, FinanceDocumentType.INCOME);
            case PAYABLE -> createDocumentBackedMovement(contact, request, FinanceDocumentType.EXPENSE);
            case COLLECTION -> createSettlementBackedMovement(contact, request, FinanceDocumentType.INCOME);
            case PAYMENT -> createSettlementBackedMovement(contact, request, FinanceDocumentType.EXPENSE);
            case ADJUSTMENT -> createAdjustmentMovement(contact, request);
        };
        movement = movementRepository.save(movement);
        refreshStoredBalance(contact);
        return toResponse(movement);
    }

    @Transactional
    public void deleteMovement(Long contactId, Long movementId, String reason) {
        Contact contact = find(contactId);
        ContactMovement movement = movementRepository.findByIdAndCompanyIdAndContactId(movementId, contact.getCompanyId(), contact.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact movement not found"));

        if (movement.getDocumentId() != null && (movement.getMovementType() == ContactMovementType.RECEIVABLE || movement.getMovementType() == ContactMovementType.PAYABLE)) {
            if (financeService.hasActiveSettlements(movement.getDocumentId())) {
                throw new BadRequestException("This document has settlements. Void the settlement movements first.");
            }
            financeService.voidDocumentDirect(movement.getDocumentId());
        } else if (movement.getSettlementId() != null && (movement.getMovementType() == ContactMovementType.COLLECTION || movement.getMovementType() == ContactMovementType.PAYMENT)) {
            if (movement.getFinancialTransactionId() != null) {
                financeService.voidTransaction(movement.getFinancialTransactionId(), new VoidTransactionRequest(reason));
            }
            if (movement.getDocumentId() != null) {
                financeService.recalculateDocumentStatusById(movement.getDocumentId());
            }
        } else if (movement.getFinancialTransactionId() != null) {
            financeService.voidTransaction(movement.getFinancialTransactionId(), new VoidTransactionRequest(reason));
        }

        movementRepository.delete(movement);
        refreshStoredBalance(contact);
    }

    public Contact find(Long id) {
        return contactRepository.findByIdAndCompanyIdAndIsActiveTrue(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private ContactMovement createDocumentBackedMovement(Contact contact, ContactMovementRequest request, FinanceDocumentType documentType) {
        validateDocumentCreationRequest(request);
        FinanceDocument document = financeService.createContactDocument(
                contact.getId(),
                documentType,
                request.categoryId(),
                request.eventId(),
                request.movementDate(),
                request.dueDate(),
                request.amount(),
                request.description(),
                request.eventId() == null ? OperationContext.CONTACT : OperationContext.EVENT
        );
        return buildMovement(
                contact,
                request,
                request.financialTransactionId(),
                document.getId(),
                null,
                request.amount()
        );
    }

    private ContactMovement createSettlementBackedMovement(Contact contact, ContactMovementRequest request, FinanceDocumentType expectedType) {
        FinanceDocument document;
        FinanceDocumentSettlement settlement;

        if (request.documentId() != null) {
            document = financeService.findDocumentForContact(contact.getId(), request.documentId(), expectedType);
            settlement = request.financialTransactionId() == null
                    ? financeService.settleContactDocument(
                            contact.getId(),
                            document.getId(),
                            expectedType,
                            new FinanceDocumentSettlementRequest(
                                    request.movementDate(),
                                    request.amount(),
                                    requiredAccountId(request),
                                    request.paymentMethodId(),
                                    request.description()
                            )
                    )
                    : financeService.attachSettlementToExistingTransaction(
                            contact.getId(),
                            document.getId(),
                            expectedType,
                            request.movementDate(),
                            request.amount(),
                            request.accountId(),
                            request.paymentMethodId(),
                            request.description(),
                            request.financialTransactionId()
                    );
        } else if (request.financialTransactionId() != null) {
            FinancialTransaction transaction = financeService.findTransaction(request.financialTransactionId(), contact.getCompanyId());
            validateTransactionAdapter(contact, request, expectedType, transaction);
            document = financeService.createContactDocument(
                    contact.getId(),
                    expectedType,
                    request.categoryId() == null ? transaction.getCategoryId() : request.categoryId(),
                    request.eventId() == null ? transaction.getEventId() : request.eventId(),
                    request.movementDate(),
                    request.dueDate(),
                    request.amount(),
                    request.description(),
                    transaction.getOperationContext()
            );
            settlement = financeService.attachSettlementToExistingTransaction(
                    contact.getId(),
                    document.getId(),
                    expectedType,
                    request.movementDate(),
                    request.amount(),
                    request.accountId() == null ? transaction.getAccountId() : request.accountId(),
                    request.paymentMethodId() == null ? transaction.getPaymentMethodId() : request.paymentMethodId(),
                    request.description(),
                    transaction.getId()
            );
        } else {
            throw new BadRequestException("Document is required for collection/payment movements");
        }

        return buildMovement(
                contact,
                request,
                settlement.getFinancialTransactionId(),
                document.getId(),
                settlement.getId(),
                financeService.remainingDocumentAmount(document)
        );
    }

    private ContactMovement createAdjustmentMovement(Contact contact, ContactMovementRequest request) {
        if (request.financialTransactionId() != null || request.documentId() != null) {
            throw new BadRequestException("Adjustment movements cannot be linked to documents or transactions");
        }
        return buildMovement(contact, request, null, null, null, null);
    }

    private ContactMovement buildMovement(
            Contact contact,
            ContactMovementRequest request,
            Long financialTransactionId,
            Long documentId,
            Long settlementId,
            BigDecimal remainingDocumentAmount
    ) {
        ContactMovement movement = new ContactMovement();
        movement.setCompanyId(contact.getCompanyId());
        movement.setContactId(contact.getId());
        movement.setFinancialTransactionId(financialTransactionId);
        movement.setDocumentId(documentId);
        movement.setSettlementId(settlementId);
        movement.setMovementDate(request.movementDate());
        movement.setMovementType(request.movementType());
        movement.setAmount(request.amount());
        movement.setBalanceDelta(request.movementType().balanceDelta(request.amount()));
        movement.setRemainingDocumentAmount(remainingDocumentAmount);
        movement.setDescription(request.description());
        movement.setCreatedByUserId(SecurityUtils.currentUser().getId());
        return movement;
    }

    private List<ContactResponse> mapContacts(List<Contact> contacts) {
        if (contacts.isEmpty()) {
            return List.of();
        }
        Long companyId = contacts.get(0).getCompanyId();
        List<Long> contactIds = contacts.stream().map(Contact::getId).toList();
        Map<Long, FinanceService.ContactOpenAmounts> openAmounts = financeService.contactOpenAmounts(contactIds);
        Map<Long, BigDecimal> adjustmentTotals = movementRepository.findByCompanyIdAndContactIdInAndMovementType(
                        companyId,
                        contactIds,
                        ContactMovementType.ADJUSTMENT
                ).stream()
                .collect(Collectors.groupingBy(
                        ContactMovement::getContactId,
                        Collectors.reducing(BigDecimal.ZERO, ContactMovement::getBalanceDelta, BigDecimal::add)
                ));
        Map<Long, BigDecimal> legacyTotals = movementRepository.findByCompanyIdAndContactIdInAndDocumentIdIsNullAndSettlementIdIsNull(companyId, contactIds)
                .stream()
                .filter(movement -> movement.getMovementType() != ContactMovementType.ADJUSTMENT)
                .collect(Collectors.groupingBy(
                        ContactMovement::getContactId,
                        Collectors.reducing(BigDecimal.ZERO, ContactMovement::getBalanceDelta, BigDecimal::add)
                ));
        Map<Long, LocalDate> lastMovementDates = movementRepository.findLastMovementDates(companyId, contactIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (LocalDate) row[1]
                ));

        return contacts.stream()
                .map(contact -> {
                    FinanceService.ContactOpenAmounts summary = openAmounts.getOrDefault(
                            contact.getId(),
                            new FinanceService.ContactOpenAmounts(BigDecimal.ZERO, BigDecimal.ZERO)
                    );
                    BigDecimal currentBalance = nvl(contact.getOpeningBalance())
                            .add(summary.openReceivableAmount())
                            .subtract(summary.openPayableAmount())
                            .add(adjustmentTotals.getOrDefault(contact.getId(), BigDecimal.ZERO))
                            .add(legacyTotals.getOrDefault(contact.getId(), BigDecimal.ZERO));
                    return new ContactResponse(
                            contact.getId(),
                            contact.getName(),
                            contact.getContactType(),
                            contact.getPhone(),
                            contact.getEmail(),
                            contact.getTaxNumber(),
                            contact.getNotes(),
                            contact.getOpeningBalance(),
                            currentBalance,
                            summary.openReceivableAmount(),
                            summary.openPayableAmount(),
                            lastMovementDates.get(contact.getId())
                    );
                })
                .toList();
    }

    private ContactMovementResponse toResponse(ContactMovement movement) {
        return new ContactMovementResponse(
                movement.getId(),
                movement.getContactId(),
                movement.getFinancialTransactionId(),
                movement.getDocumentId(),
                movement.getSettlementId(),
                movement.getMovementDate(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getBalanceDelta(),
                movement.getRemainingDocumentAmount(),
                movement.getDescription()
        );
    }

    private void validateDocumentCreationRequest(ContactMovementRequest request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("Category is required");
        }
        if (request.financialTransactionId() != null) {
            throw new BadRequestException("Receivable/payable movements cannot be linked to an existing transaction");
        }
        if (request.documentId() != null) {
            throw new BadRequestException("Document id cannot be sent for receivable/payable creation");
        }
    }

    private void validateTransactionAdapter(
            Contact contact,
            ContactMovementRequest request,
            FinanceDocumentType expectedType,
            FinancialTransaction transaction
    ) {
        if (transaction.getStatus() == com.dada.eventmanagement.common.enums.FinancialTransactionStatus.VOIDED) {
            throw new BadRequestException("Voided transaction cannot be linked");
        }
        if (transaction.getContactId() != null && !transaction.getContactId().equals(contact.getId())) {
            throw new BadRequestException("Transaction belongs to another contact");
        }
        if (transaction.getTransactionType() != expectedType.transactionType()) {
            throw new BadRequestException("Transaction type does not match movement type");
        }
        if (transaction.getAmount().compareTo(request.amount()) != 0) {
            throw new BadRequestException("Transaction amount must match movement amount");
        }
        if (request.categoryId() == null && transaction.getCategoryId() == null) {
            throw new BadRequestException("Category is required");
        }
    }

    private Long requiredAccountId(ContactMovementRequest request) {
        if (request.accountId() == null) {
            throw new BadRequestException("Account is required for collection/payment movements");
        }
        return request.accountId();
    }

    private void refreshStoredBalance(Contact contact) {
        BigDecimal recalculated = mapContacts(List.of(contact)).get(0).currentBalance();
        contact.setCurrentBalance(recalculated);
        contactRepository.save(contact);
    }

    private BigDecimal calculateCurrentBalance(Contact contact, BigDecimal openingBalance) {
        FinanceService.ContactOpenAmounts summary = financeService.contactOpenAmounts(List.of(contact.getId()))
                .getOrDefault(contact.getId(), new FinanceService.ContactOpenAmounts(BigDecimal.ZERO, BigDecimal.ZERO));
        BigDecimal adjustmentTotal = movementRepository.findByCompanyIdAndContactIdInAndMovementType(
                        contact.getCompanyId(),
                        List.of(contact.getId()),
                        ContactMovementType.ADJUSTMENT
                ).stream()
                .map(ContactMovement::getBalanceDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal legacyTotal = movementRepository.findByCompanyIdAndContactIdInAndDocumentIdIsNullAndSettlementIdIsNull(
                        contact.getCompanyId(),
                        List.of(contact.getId())
                ).stream()
                .filter(movement -> movement.getMovementType() != ContactMovementType.ADJUSTMENT)
                .map(ContactMovement::getBalanceDelta)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return openingBalance
                .add(summary.openReceivableAmount())
                .subtract(summary.openPayableAmount())
                .add(adjustmentTotal)
                .add(legacyTotal);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
