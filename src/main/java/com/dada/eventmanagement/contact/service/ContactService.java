package com.dada.eventmanagement.contact.service;

import com.dada.eventmanagement.common.enums.ContactType;
import com.dada.eventmanagement.common.enums.ContactMovementType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
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
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.VoidTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import java.math.BigDecimal;
import java.util.List;
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
        return rows.stream().map(this::toResponse).toList();
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
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse update(Long id, ContactRequest request) {
        Contact contact = find(id);
        BigDecimal oldOpening = contact.getOpeningBalance();
        BigDecimal newOpening = nvl(request.openingBalance());
        contact.setName(request.name().trim());
        contact.setContactType(request.contactType());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setTaxNumber(request.taxNumber());
        contact.setNotes(request.notes());
        contact.setOpeningBalance(newOpening);
        contact.setCurrentBalance(contact.getCurrentBalance().subtract(oldOpening).add(newOpening));
        return toResponse(contactRepository.save(contact));
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

    @Transactional
    public ContactMovementResponse createMovement(Long contactId, ContactMovementRequest request) {
        Contact contact = find(contactId);
        BigDecimal delta = request.movementType().balanceDelta(request.amount());
        Long financialTransactionId = request.financialTransactionId();
        if (financialTransactionId == null && createsFinancialTransaction(request.movementType())) {
            validateFinancialFields(request);
            FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                    request.eventId(),
                    request.accountId(),
                    request.paymentMethodId(),
                    request.categoryId(),
                    request.revenueChannelId(),
                    contact.getId(),
                    request.movementType() == ContactMovementType.COLLECTION ? FinancialTransactionType.INCOME : FinancialTransactionType.EXPENSE,
                    request.movementDate(),
                    request.amount(),
                    null,
                    request.description()
            ));
            financialTransactionId = tx.getId();
        }
        ContactMovement movement = new ContactMovement();
        movement.setCompanyId(contact.getCompanyId());
        movement.setContactId(contact.getId());
        movement.setFinancialTransactionId(financialTransactionId);
        movement.setMovementDate(request.movementDate());
        movement.setMovementType(request.movementType());
        movement.setAmount(request.amount());
        movement.setBalanceDelta(delta);
        movement.setDescription(request.description());
        movement.setCreatedByUserId(SecurityUtils.currentUser().getId());
        contact.setCurrentBalance(contact.getCurrentBalance().add(delta));
        contactRepository.save(contact);
        return toResponse(movementRepository.save(movement));
    }

    @Transactional
    public void deleteMovement(Long contactId, Long movementId, String reason) {
        Contact contact = find(contactId);
        ContactMovement movement = movementRepository.findByIdAndCompanyIdAndContactId(movementId, contact.getCompanyId(), contact.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact movement not found"));
        if (movement.getFinancialTransactionId() != null) {
            financeService.voidTransaction(movement.getFinancialTransactionId(), new VoidTransactionRequest(reason));
        }
        contact.setCurrentBalance(contact.getCurrentBalance().subtract(movement.getBalanceDelta()));
        contactRepository.save(contact);
        movementRepository.delete(movement);
    }

    private boolean createsFinancialTransaction(ContactMovementType type) {
        return type == ContactMovementType.COLLECTION || type == ContactMovementType.PAYMENT;
    }

    private void validateFinancialFields(ContactMovementRequest request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("Category is required for collection/payment movements");
        }
    }

    public Contact find(Long id) {
        return contactRepository.findByIdAndCompanyIdAndIsActiveTrue(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private ContactResponse toResponse(Contact c) {
        return new ContactResponse(c.getId(), c.getName(), c.getContactType(), c.getPhone(), c.getEmail(), c.getTaxNumber(), c.getNotes(), c.getOpeningBalance(), c.getCurrentBalance());
    }

    private ContactMovementResponse toResponse(ContactMovement m) {
        return new ContactMovementResponse(m.getId(), m.getContactId(), m.getFinancialTransactionId(), m.getMovementDate(), m.getMovementType(), m.getAmount(), m.getBalanceDelta(), m.getDescription());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
