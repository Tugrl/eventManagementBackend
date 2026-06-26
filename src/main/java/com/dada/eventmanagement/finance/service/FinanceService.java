package com.dada.eventmanagement.finance.service;

import com.dada.eventmanagement.common.enums.*;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.contact.entity.Contact;
import com.dada.eventmanagement.contact.repository.ContactRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.reservation.entity.Reservation;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import com.dada.eventmanagement.report.entity.EventClosingReport;
import com.dada.eventmanagement.report.repository.EventClosingReportRepository;
import com.dada.eventmanagement.finance.dto.*;
import com.dada.eventmanagement.finance.entity.*;
import com.dada.eventmanagement.finance.entity.PaymentMethod;
import com.dada.eventmanagement.finance.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {
    private final FinancialAccountRepository accountRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final RevenueChannelRepository revenueChannelRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final DailyCashReportRepository dailyCashReportRepository;
    private final EventRepository eventRepository;
    private final ContactRepository contactRepository;
    private final ReservationRepository reservationRepository;
    private final EventClosingReportRepository eventClosingReportRepository;
    private final FinanceDocumentRepository financeDocumentRepository;
    private final FinanceDocumentSettlementRepository financeDocumentSettlementRepository;

    public FinanceService(
            FinancialAccountRepository accountRepository,
            PaymentMethodRepository paymentMethodRepository,
            FinancialCategoryRepository categoryRepository,
            RevenueChannelRepository revenueChannelRepository,
            FinancialTransactionRepository transactionRepository,
            DailyCashReportRepository dailyCashReportRepository,
            EventRepository eventRepository,
            ContactRepository contactRepository,
            ReservationRepository reservationRepository,
            EventClosingReportRepository eventClosingReportRepository,
            FinanceDocumentRepository financeDocumentRepository,
            FinanceDocumentSettlementRepository financeDocumentSettlementRepository
    ) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.categoryRepository = categoryRepository;
        this.revenueChannelRepository = revenueChannelRepository;
        this.transactionRepository = transactionRepository;
        this.dailyCashReportRepository = dailyCashReportRepository;
        this.eventRepository = eventRepository;
        this.contactRepository = contactRepository;
        this.reservationRepository = reservationRepository;
        this.eventClosingReportRepository = eventClosingReportRepository;
        this.financeDocumentRepository = financeDocumentRepository;
        this.financeDocumentSettlementRepository = financeDocumentSettlementRepository;
    }

    public List<FinancialAccountResponse> accounts() {
        Long companyId = SecurityUtils.currentCompanyId();
        return accountRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public FinancialAccountResponse createAccount(FinancialAccountRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultAccounts(companyId, null);
        }
        FinancialAccount account = new FinancialAccount();
        account.setCompanyId(companyId);
        account.setName(request.name().trim());
        account.setAccountType(request.accountType());
        account.setOpeningBalance(request.openingBalance());
        account.setCurrentBalance(request.openingBalance());
        account.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        account.setIsActive(true);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public FinancialAccountResponse updateAccount(Long id, FinancialAccountRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinancialAccount account = findAccount(id, companyId);
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaultAccounts(companyId, id);
        }
        BigDecimal balanceDelta = request.openingBalance().subtract(account.getOpeningBalance());
        account.setName(request.name().trim());
        account.setAccountType(request.accountType());
        account.setOpeningBalance(request.openingBalance());
        account.setCurrentBalance(account.getCurrentBalance().add(balanceDelta));
        account.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinancialAccount account = findAccount(id, companyId);
        if (Boolean.TRUE.equals(account.getIsDefault())) {
            throw new BadRequestException("Default account cannot be deleted");
        }
        account.setIsActive(false);
        accountRepository.save(account);
    }

    public List<PaymentMethodResponse> paymentMethods() {
        Long companyId = SecurityUtils.currentCompanyId();
        return paymentMethodRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream().map(this::toResponse).toList();
    }

    public List<FinancialCategoryResponse> categories(FinancialCategoryType type) {
        Long companyId = SecurityUtils.currentCompanyId();
        List<FinancialCategory> rows = type == null
                ? categoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId)
                : categoryRepository.findByCompanyIdAndCategoryTypeAndIsActiveTrueOrderByNameAsc(companyId, type);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public FinancialCategoryResponse createCategory(FinancialCategoryRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        validateParentCategory(companyId, request.parentId());
        FinancialCategory category = new FinancialCategory();
        category.setCompanyId(companyId);
        category.setParentId(request.parentId());
        category.setName(request.name().trim());
        category.setCategoryType(request.categoryType());
        category.setScope(request.scope());
        category.setOperationGroup(request.operationGroup());
        category.setDescription(request.description());
        category.setIsDefault(false);
        category.setIsActive(true);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public FinancialCategoryResponse updateCategory(Long id, FinancialCategoryRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinancialCategory category = findCategory(id, companyId);
        validateParentCategory(companyId, request.parentId());
        category.setParentId(request.parentId());
        category.setName(request.name().trim());
        category.setCategoryType(request.categoryType());
        category.setScope(request.scope());
        category.setOperationGroup(request.operationGroup());
        category.setDescription(request.description());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinancialCategory category = findCategory(id, companyId);
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    public List<RevenueChannelResponse> revenueChannels() {
        Long companyId = SecurityUtils.currentCompanyId();
        return revenueChannelRepository.findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RevenueChannelResponse createRevenueChannel(RevenueChannelRequest request) {
        RevenueChannel channel = new RevenueChannel();
        channel.setCompanyId(SecurityUtils.currentCompanyId());
        channel.setName(request.name().trim());
        channel.setChannelType(request.channelType());
        channel.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        channel.setIsDefault(false);
        channel.setIsActive(true);
        return toResponse(revenueChannelRepository.save(channel));
    }

    @Transactional
    public RevenueChannelResponse updateRevenueChannel(Long id, RevenueChannelRequest request) {
        RevenueChannel channel = findRevenueChannel(id, SecurityUtils.currentCompanyId());
        channel.setName(request.name().trim());
        channel.setChannelType(request.channelType());
        channel.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        return toResponse(revenueChannelRepository.save(channel));
    }

    @Transactional
    public void deleteRevenueChannel(Long id) {
        RevenueChannel channel = findRevenueChannel(id, SecurityUtils.currentCompanyId());
        channel.setIsActive(false);
        revenueChannelRepository.save(channel);
    }

    public List<FinancialTransactionResponse> transactions(
            LocalDate startDate,
            LocalDate endDate,
            FinancialTransactionType transactionType,
            Long accountId,
            Long categoryId,
            Long eventId,
            Long contactId,
            OperationSource operationSource,
            FinancialTransactionStatus status
    ) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start : endDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        return mapTransactions(transactionRepository.search(
                companyId, start, end, transactionType, accountId, categoryId, eventId, contactId,
                operationSource, status == null ? FinancialTransactionStatus.ACTIVE : status
        ));
    }

    public List<FinanceDocumentResponse> documents(
            FinanceDocumentType documentType,
            FinanceDocumentStatus status,
            Long categoryId,
            Long contactId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Long companyId = SecurityUtils.currentCompanyId();
        DateRange range = normalizeRange(startDate, endDate);
        List<FinanceDocument> documents = financeDocumentRepository.search(
                companyId, documentType, status, categoryId, contactId, range.start(), range.end()
        );
        return mapDocuments(documents);
    }

    public FinanceDocumentResponse documentDetail(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = findDocument(id, companyId);
        return mapDocuments(List.of(document)).get(0);
    }

    public List<FinanceDocumentResponse> contactDocuments(
            Long contactId,
            FinanceDocumentType documentType,
            FinanceDocumentStatus status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Long companyId = SecurityUtils.currentCompanyId();
        contactRepository.findByIdAndCompanyIdAndIsActiveTrue(contactId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        return documents(documentType, status, null, contactId, startDate, endDate);
    }

    @Transactional
    public FinanceDocumentResponse createDocument(FinanceDocumentRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = buildDocument(
                companyId,
                request.documentType(),
                request.categoryId(),
                request.contactId(),
                request.eventId(),
                request.issueDate(),
                request.dueDate(),
                request.amount(),
                request.description(),
                OperationContext.COMPANY
        );
        return mapDocuments(List.of(financeDocumentRepository.save(document))).get(0);
    }

    @Transactional
    public FinanceDocumentResponse updateDocument(Long id, FinanceDocumentRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = findDocument(id, companyId);
        if (document.getStatus() == FinanceDocumentStatus.VOIDED) {
            throw new BadRequestException("Voided document cannot be updated");
        }
        FinancialCategory category = validateDocumentCategory(companyId, request.categoryId(), request.documentType());
        BigDecimal settledAmount = activeSettledAmount(companyId, List.of(document)).get(document.getId());
        if (settledAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (document.getDocumentType() != request.documentType()) {
                throw new BadRequestException("Document type cannot change after settlement");
            }
            if (!document.getCategoryId().equals(request.categoryId())) {
                throw new BadRequestException("Category cannot change after settlement");
            }
            if (request.amount().compareTo(settledAmount) < 0) {
                throw new BadRequestException("Document amount cannot be lower than settled amount");
            }
        }
        if (request.contactId() != null) {
            contactRepository.findByIdAndCompanyIdAndIsActiveTrue(request.contactId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        }
        if (request.eventId() != null) {
            eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(request.eventId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        }
        document.setDocumentType(request.documentType());
        document.setCategoryId(category.getId());
        document.setContactId(request.contactId());
        document.setEventId(request.eventId());
        document.setIssueDate(request.issueDate());
        document.setDueDate(request.dueDate());
        document.setAmount(request.amount());
        document.setDescription(blankToNull(request.description()));
        document.setOperationGroup(category.getOperationGroup());
        recalculateDocumentStatus(document, settledAmount);
        return mapDocuments(List.of(financeDocumentRepository.save(document))).get(0);
    }

    @Transactional
    public FinanceDocumentSettlementResponse createDocumentSettlement(Long id, FinanceDocumentSettlementRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = findDocument(id, companyId);
        FinanceDocumentSettlement saved = createSettlement(document, request, null);
        FinancialTransactionStatus status = saved.getFinancialTransactionId() == null
                ? FinancialTransactionStatus.ACTIVE
                : findTransaction(saved.getFinancialTransactionId(), companyId).getStatus();
        return toResponse(saved, status);
    }

    public Map<Long, ContactOpenAmounts> contactOpenAmounts(List<Long> contactIds) {
        Long companyId = SecurityUtils.currentCompanyId();
        if (contactIds == null || contactIds.isEmpty()) {
            return Map.of();
        }
        List<FinanceDocument> documents = financeDocumentRepository.findByCompanyIdAndContactIdInAndStatusNot(
                companyId,
                contactIds,
                FinanceDocumentStatus.VOIDED
        );
        Map<Long, BigDecimal> settledAmounts = activeSettledAmount(companyId, documents);
        Map<Long, BigDecimal> receivableTotals = contactIds.stream()
                .collect(Collectors.toMap(Function.identity(), id -> BigDecimal.ZERO));
        Map<Long, BigDecimal> payableTotals = contactIds.stream()
                .collect(Collectors.toMap(Function.identity(), id -> BigDecimal.ZERO));
        for (FinanceDocument document : documents) {
            if (document.getContactId() == null) {
                continue;
            }
            BigDecimal remaining = document.getAmount().subtract(settledAmounts.getOrDefault(document.getId(), BigDecimal.ZERO));
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (document.getDocumentType() == FinanceDocumentType.INCOME) {
                receivableTotals.put(document.getContactId(), receivableTotals.getOrDefault(document.getContactId(), BigDecimal.ZERO).add(remaining));
            } else {
                payableTotals.put(document.getContactId(), payableTotals.getOrDefault(document.getContactId(), BigDecimal.ZERO).add(remaining));
            }
        }
        return contactIds.stream().collect(Collectors.toMap(
                Function.identity(),
                id -> new ContactOpenAmounts(
                        receivableTotals.getOrDefault(id, BigDecimal.ZERO),
                        payableTotals.getOrDefault(id, BigDecimal.ZERO)
                )
        ));
    }

    @Transactional
    public FinanceDocument createContactDocument(
            Long contactId,
            FinanceDocumentType documentType,
            Long categoryId,
            Long eventId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal amount,
            String description,
            OperationContext operationContext
    ) {
        Long companyId = SecurityUtils.currentCompanyId();
        contactRepository.findByIdAndCompanyIdAndIsActiveTrue(contactId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        FinanceDocument document = buildDocument(
                companyId,
                documentType,
                categoryId,
                contactId,
                eventId,
                issueDate,
                dueDate,
                amount,
                description,
                operationContext == null ? OperationContext.CONTACT : operationContext
        );
        return financeDocumentRepository.save(document);
    }

    @Transactional
    public FinanceDocumentSettlement settleContactDocument(
            Long contactId,
            Long documentId,
            FinanceDocumentType expectedType,
            FinanceDocumentSettlementRequest request
    ) {
        return createSettlement(validateContactDocument(contactId, documentId, expectedType), request, null);
    }

    @Transactional
    public FinanceDocumentSettlement attachSettlementToExistingTransaction(
            Long contactId,
            Long documentId,
            FinanceDocumentType expectedType,
            LocalDate settlementDate,
            BigDecimal amount,
            Long accountId,
            Long paymentMethodId,
            String notes,
            Long financialTransactionId
    ) {
        FinanceDocument document = validateContactDocument(contactId, documentId, expectedType);
        if (financialTransactionId == null) {
            throw new BadRequestException("Financial transaction is required");
        }
        FinancialTransaction transaction = findTransaction(financialTransactionId, SecurityUtils.currentCompanyId());
        if (transaction.getStatus() == FinancialTransactionStatus.VOIDED) {
            throw new BadRequestException("Voided transaction cannot be attached as settlement");
        }
        if (transaction.getTransactionType() != expectedType.transactionType()) {
            throw new BadRequestException("Transaction type does not match document type");
        }
        if (transaction.getAmount().compareTo(amount) != 0) {
            throw new BadRequestException("Existing transaction amount must match settlement amount");
        }
        if (!financeDocumentSettlementRepository.findByCompanyIdAndFinancialTransactionId(SecurityUtils.currentCompanyId(), transaction.getId()).isEmpty()) {
            throw new BadRequestException("This transaction is already linked to a document settlement");
        }
        return createSettlement(
                document,
                new FinanceDocumentSettlementRequest(
                        settlementDate,
                        amount,
                        accountId == null ? transaction.getAccountId() : accountId,
                        paymentMethodId == null ? transaction.getPaymentMethodId() : paymentMethodId,
                        notes
                ),
                transaction
        );
    }

    public FinanceDocument findDocumentForContact(Long contactId, Long documentId, FinanceDocumentType expectedType) {
        return validateContactDocument(contactId, documentId, expectedType);
    }

    public FinancialTransaction findTransaction(Long id, Long companyId) {
        return transactionRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial transaction not found"));
    }

    public BigDecimal remainingDocumentAmount(FinanceDocument document) {
        BigDecimal settledAmount = activeSettledAmount(document.getCompanyId(), List.of(document)).getOrDefault(document.getId(), BigDecimal.ZERO);
        return document.getAmount().subtract(settledAmount);
    }

    public boolean hasActiveSettlements(Long documentId) {
        FinanceDocument document = findDocument(documentId, SecurityUtils.currentCompanyId());
        return activeSettledAmount(document.getCompanyId(), List.of(document))
                .getOrDefault(document.getId(), BigDecimal.ZERO)
                .compareTo(BigDecimal.ZERO) > 0;
    }

    @Transactional
    public void voidDocumentDirect(Long documentId) {
        FinanceDocument document = findDocument(documentId, SecurityUtils.currentCompanyId());
        document.setStatus(FinanceDocumentStatus.VOIDED);
        financeDocumentRepository.save(document);
    }

    @Transactional
    public void recalculateDocumentStatusById(Long documentId) {
        recalculateDocumentStatus(findDocument(documentId, SecurityUtils.currentCompanyId()));
    }

    @Transactional
    public void voidDocument(Long id, VoidTransactionRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = findDocument(id, companyId);
        if (document.getStatus() == FinanceDocumentStatus.VOIDED) {
            throw new BadRequestException("Document is already voided");
        }
        List<FinanceDocumentSettlement> settlements = financeDocumentSettlementRepository.findByCompanyIdAndDocumentIdOrderBySettlementDateDescIdDesc(companyId, document.getId());
        List<Long> transactionIds = settlements.stream().map(FinanceDocumentSettlement::getFinancialTransactionId).filter(v -> v != null).toList();
        Map<Long, FinancialTransaction> txMap = transactionIds.isEmpty()
                ? Map.of()
                : transactionRepository.findByCompanyIdAndIdIn(companyId, transactionIds).stream()
                .collect(Collectors.toMap(FinancialTransaction::getId, Function.identity()));
        for (FinanceDocumentSettlement settlement : settlements) {
            FinancialTransaction tx = settlement.getFinancialTransactionId() == null ? null : txMap.get(settlement.getFinancialTransactionId());
            if (tx != null && tx.getStatus() == FinancialTransactionStatus.ACTIVE) {
                voidTransaction(tx.getId(), request);
            }
        }
        document.setStatus(FinanceDocumentStatus.VOIDED);
        financeDocumentRepository.save(document);
    }

    public FinanceDashboardSummaryResponse dashboardSummary() {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate now = LocalDate.now();
        DateRange range = new DateRange(now.withDayOfMonth(1), now);
        List<FinancialTransaction> periodTransactions = activeTransactions(companyId, range);
        List<FinanceDocument> activeDocuments = financeDocumentRepository.findByCompanyIdAndStatusNotOrderByIssueDateDescIdDesc(companyId, FinanceDocumentStatus.VOIDED);
        Map<Long, BigDecimal> settledAmounts = activeSettledAmount(companyId, activeDocuments);
        BigDecimal paidIncome = sum(periodTransactions, FinancialTransactionType.INCOME);
        BigDecimal paidExpense = sum(periodTransactions, FinancialTransactionType.EXPENSE);
        BigDecimal openReceivables = activeDocuments.stream()
                .filter(document -> document.getDocumentType() == FinanceDocumentType.INCOME)
                .map(document -> document.getAmount().subtract(settledAmounts.getOrDefault(document.getId(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openPayables = activeDocuments.stream()
                .filter(document -> document.getDocumentType() == FinanceDocumentType.EXPENSE)
                .map(document -> document.getAmount().subtract(settledAmounts.getOrDefault(document.getId(), BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<FinancialAccount> accounts = accountRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId);
        BigDecimal cashBalance = accounts.stream().filter(account -> account.getAccountType() == AccountType.CASH).map(FinancialAccount::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal bankBalance = accounts.stream().filter(account -> account.getAccountType() == AccountType.BANK).map(FinancialAccount::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cardBalance = accounts.stream().filter(account -> account.getAccountType() == AccountType.CARD).map(FinancialAccount::getCurrentBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinanceDashboardSummaryResponse(paidIncome, paidExpense, openReceivables, openPayables, cashBalance, bankBalance, cardBalance);
    }

    public List<FinancialTransactionResponse> eventIncomeTransactions(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapTransactions(transactionRepository.findByCompanyIdAndEventIdAndTransactionTypeAndStatusOrderByTransactionDateDescIdDesc(
                companyId,
                eventId,
                FinancialTransactionType.INCOME,
                FinancialTransactionStatus.ACTIVE
        ));
    }

    public FinancialTransactionResponse toTransactionResponse(FinancialTransaction transaction) {
        return mapTransactions(List.of(transaction)).get(0);
    }

    @Transactional
    public FinancialTransactionResponse createTransaction(FinancialTransactionRequest request) {
        return mapTransactions(List.of(createTransactionEntity(request))).get(0);
    }

    @Transactional
    public FinancialTransaction createTransactionEntity(FinancialTransactionRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        OperationSource operationSource = request.operationSource() == null ? defaultOperationSource(request) : request.operationSource();
        if (request.referenceId() != null && idempotencyProtected(operationSource)) {
            transactionRepository.findFirstByCompanyIdAndOperationSourceAndReferenceIdAndStatus(
                    companyId, operationSource, request.referenceId(), FinancialTransactionStatus.ACTIVE
            ).ifPresent(existing -> {
                throw new BadRequestException("This source record is already posted to finance");
            });
        }
        ensureDateIsOpen(companyId, request.transactionDate());
        FinancialCategory category = findCategory(request.categoryId(), companyId);
        if (category.getCategoryType() != request.transactionType().categoryType()) {
            throw new BadRequestException("Category type must match transaction type");
        }
        if (request.eventId() != null) {
            eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(request.eventId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        }
        Long accountId = resolveAccountId(companyId, request.accountId(), request.transactionType());
        Long paymentMethodId = resolvePaymentMethodId(companyId, request.paymentMethodId());
        if (request.revenueChannelId() != null) {
            findRevenueChannel(request.revenueChannelId(), companyId);
        }

        FinancialTransaction tx = new FinancialTransaction();
        tx.setCompanyId(companyId);
        tx.setEventId(request.eventId());
        tx.setAccountId(accountId);
        tx.setPaymentMethodId(paymentMethodId);
        tx.setCategoryId(request.categoryId());
        tx.setRevenueChannelId(request.revenueChannelId());
        tx.setContactId(request.contactId());
        tx.setTransactionType(request.transactionType());
        tx.setTransactionDate(request.transactionDate());
        tx.setAmount(request.amount());
        tx.setGuestCount(request.guestCount());
        tx.setDescription(request.description());
        tx.setOperationSource(operationSource);
        tx.setOperationContext(request.operationContext() == null ? defaultOperationContext(request) : request.operationContext());
        tx.setReferenceId(request.referenceId());
        tx.setCreatedByUserId(SecurityUtils.currentUser().getId());
        tx.setStatus(FinancialTransactionStatus.ACTIVE);
        updateAccountBalance(accountId, companyId, request.transactionType(), request.amount());
        return transactionRepository.save(tx);
    }

    private boolean idempotencyProtected(OperationSource source) {
        return source == OperationSource.RESERVATION_PAYMENT
                || source == OperationSource.STAFF_PAYOUT
                || source == OperationSource.COMPANY_EXPENSE
                || source == OperationSource.EVENT_CLOSING_COST;
    }

    @Transactional
    public void voidTransaction(Long id, VoidTransactionRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinancialTransaction tx = transactionRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial transaction not found"));
        if (tx.getStatus() == FinancialTransactionStatus.VOIDED) {
            throw new BadRequestException("Transaction is already voided");
        }
        ensureDateIsOpen(companyId, tx.getTransactionDate());
        tx.setStatus(FinancialTransactionStatus.VOIDED);
        tx.setVoidReason(request.reason().trim());
        tx.setVoidedByUserId(SecurityUtils.currentUser().getId());
        tx.setVoidedAt(LocalDateTime.now());
        updateAccountBalance(tx.getAccountId(), companyId, tx.getTransactionType().reverse(), tx.getAmount());
        transactionRepository.save(tx);
        if (tx.getOperationSource() == OperationSource.FINANCE_DOCUMENT_SETTLEMENT && tx.getReferenceId() != null) {
            financeDocumentSettlementRepository.findByIdAndCompanyId(tx.getReferenceId(), companyId)
                    .ifPresent(settlement -> recalculateDocumentStatus(findDocument(settlement.getDocumentId(), companyId)));
        }
        for (FinanceDocumentSettlement settlement : financeDocumentSettlementRepository.findByCompanyIdAndFinancialTransactionId(companyId, tx.getId())) {
            recalculateDocumentStatus(findDocument(settlement.getDocumentId(), companyId));
        }
    }

    public DailyCashSummaryResponse dailySummary(LocalDate date) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        CashTotals totals = cashTotals(companyId, reportDate);
        boolean closed = dailyCashReportRepository.findByCompanyIdAndReportDate(companyId, reportDate)
                .map(report -> report.getStatus() == DailyCashReportStatus.CLOSED)
                .orElse(false);
        return new DailyCashSummaryResponse(
                reportDate,
                totals.totalIncome(),
                totals.totalExpense(),
                totals.cashIncome(),
                totals.cardIncome(),
                totals.bankIncome(),
                totals.currentAccountIncome(),
                totals.cashExpense(),
                totals.totalIncome().subtract(totals.totalExpense()),
                closed
        );
    }

    @Transactional
    public DailyCashReportResponse closeDailyCash(DailyCashCloseRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        DailyCashReport report = dailyCashReportRepository.findByCompanyIdAndReportDate(companyId, request.reportDate())
                .map(existing -> {
                    if (existing.getStatus() == DailyCashReportStatus.CLOSED) {
                        throw new BadRequestException("This day is already closed");
                    }
                    return existing;
                })
                .orElseGet(DailyCashReport::new);
        CashTotals totals = cashTotals(companyId, request.reportDate());
        BigDecimal managementCashIn = nvl(request.managementCashIn());
        BigDecimal managementCashOut = nvl(request.managementCashOut());
        BigDecimal expectedCash = nvl(request.openingCash())
                .add(managementCashIn)
                .add(totals.cashIncome())
                .subtract(totals.cashExpense())
                .subtract(managementCashOut);
        BigDecimal actualCash = nvl(request.actualCash());

        report.setCompanyId(companyId);
        report.setReportDate(request.reportDate());
        report.setOpeningCash(nvl(request.openingCash()));
        report.setManagementCashIn(managementCashIn);
        report.setCashIncome(totals.cashIncome());
        report.setCardIncome(totals.cardIncome());
        report.setBankIncome(totals.bankIncome());
        report.setCurrentAccountIncome(totals.currentAccountIncome());
        report.setCashExpense(totals.cashExpense());
        report.setTotalIncome(totals.totalIncome());
        report.setTotalExpense(totals.totalExpense());
        report.setExpectedCash(expectedCash);
        report.setActualCash(actualCash);
        report.setCashDifference(actualCash.subtract(expectedCash));
        report.setManagementCashOut(managementCashOut);
        report.setNotes(request.notes());
        report.setStatus(DailyCashReportStatus.CLOSED);
        report.setClosedByUserId(SecurityUtils.currentUser().getId());
        DailyCashReportResponse response = toResponse(dailyCashReportRepository.save(report));
        markEventsCompleted(companyId, request.reportDate());
        return response;
    }

    @Transactional
    public DailyCashReportResponse reopenDailyCash(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        DailyCashReport report = dailyCashReportRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily cash report not found"));
        if (report.getStatus() == DailyCashReportStatus.REOPENED) {
            throw new BadRequestException("This day is already reopened");
        }
        report.setStatus(DailyCashReportStatus.REOPENED);
        return toResponse(dailyCashReportRepository.save(report));
    }

    public List<DailyCashReportResponse> cashReports(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return dailyCashReportRepository.findByCompanyIdAndReportDateBetweenOrderByReportDateDesc(companyId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    public EventProfitSummaryResponse eventProfit(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        BigDecimal income = transactionRepository.sumByEventAndType(companyId, eventId, FinancialTransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumByEventAndType(companyId, eventId, FinancialTransactionType.EXPENSE);
        return new EventProfitSummaryResponse(eventId, income, expense, income.subtract(expense));
    }

    public FinanceOverviewReportResponse overviewReport(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        DateRange range = normalizeRange(startDate, endDate);
        List<FinancialTransaction> rows = activeTransactions(companyId, range);
        BigDecimal income = sum(rows, FinancialTransactionType.INCOME);
        BigDecimal expense = sum(rows, FinancialTransactionType.EXPENSE);
        BigDecimal cashDifference = dailyCashReportRepository
                .findByCompanyIdAndReportDateBetweenOrderByReportDateDesc(companyId, range.start(), range.end())
                .stream()
                .map(DailyCashReport::getCashDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FinanceOverviewReportResponse(range.start(), range.end(), income, expense, income.subtract(expense), cashDifference);
    }

    public List<CategoryReportRowResponse> categoryReport(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        DateRange range = normalizeRange(startDate, endDate);
        Map<Long, FinancialCategory> categoryMap = categoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(FinancialCategory::getId, Function.identity()));
        Map<Long, List<FinancialTransaction>> grouped = activeTransactions(companyId, range).stream()
                .collect(Collectors.groupingBy(FinancialTransaction::getCategoryId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal income = sum(entry.getValue(), FinancialTransactionType.INCOME);
                    BigDecimal expense = sum(entry.getValue(), FinancialTransactionType.EXPENSE);
                    FinancialCategory category = categoryMap.get(entry.getKey());
                    return new CategoryReportRowResponse(
                            entry.getKey(),
                            category == null ? "Kategori yok" : category.getName(),
                            income,
                            expense,
                            income.subtract(expense)
                    );
                })
                .sorted(Comparator.comparing(row -> row.income().add(row.expense()), Comparator.reverseOrder()))
                .toList();
    }

    public List<RevenueChannelReportRowResponse> revenueChannelReport(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        DateRange range = normalizeRange(startDate, endDate);
        Map<Long, RevenueChannel> channelMap = revenueChannelRepository.findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(companyId).stream()
                .collect(Collectors.toMap(RevenueChannel::getId, Function.identity()));
        Map<Long, List<FinancialTransaction>> grouped = activeTransactions(companyId, range).stream()
                .filter(row -> row.getTransactionType() == FinancialTransactionType.INCOME)
                .filter(row -> row.getRevenueChannelId() != null)
                .collect(Collectors.groupingBy(FinancialTransaction::getRevenueChannelId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    BigDecimal amount = entry.getValue().stream().map(FinancialTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    int guestCount = entry.getValue().stream().map(FinancialTransaction::getGuestCount).filter(v -> v != null).mapToInt(Integer::intValue).sum();
                    RevenueChannel channel = channelMap.get(entry.getKey());
                    return new RevenueChannelReportRowResponse(
                            entry.getKey(),
                            channel == null ? "Kanal yok" : channel.getName(),
                            amount,
                            guestCount
                    );
                })
                .sorted(Comparator.comparing(RevenueChannelReportRowResponse::amount, Comparator.reverseOrder()))
                .toList();
    }

    public List<EventProfitReportRowResponse> eventProfitReport(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        DateRange range = normalizeRange(startDate, endDate);
        List<Event> events = eventRepository.findByCompanyIdAndEventDateBetweenAndIsDeletedFalseOrderByEventDateAsc(
                companyId,
                range.start(),
                range.end()
        );
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));
        Map<Long, List<FinancialTransaction>> grouped = activeTransactions(companyId, range).stream()
                .filter(row -> row.getEventId() != null && eventMap.containsKey(row.getEventId()))
                .collect(Collectors.groupingBy(FinancialTransaction::getEventId));
        Map<Long, List<Reservation>> reservationsByEvent = reservationRepository.findByCompanyIdAndEventIdIn(companyId, eventIds).stream()
                .collect(Collectors.groupingBy(Reservation::getEventId));
        Map<Long, Boolean> closingReportByEvent = eventClosingReportRepository.findByCompanyIdAndEventIdIn(companyId, eventIds).stream()
                .collect(Collectors.toMap(EventClosingReport::getEventId, report -> Boolean.TRUE));

        List<EventProfitReportRowResponse> rows = new ArrayList<>();
        for (Event event : events) {
            List<FinancialTransaction> transactions = grouped.getOrDefault(event.getId(), List.of());
            List<Reservation> reservations = reservationsByEvent.getOrDefault(event.getId(), List.of());
            int reservationCount = 0;
            int pendingDepositCount = 0;
            int actualGuestCount = 0;
            BigDecimal pendingDepositAmount = BigDecimal.ZERO;
            BigDecimal minimumDepositAmount = MathUtils.money(event.getMinimumDepositAmount());
            for (Reservation reservation : reservations) {
                if (reservation.getReservationStatus() != com.dada.eventmanagement.common.enums.ReservationStatus.ACTIVE
                        && reservation.getReservationStatus() != com.dada.eventmanagement.common.enums.ReservationStatus.COMPLETED) {
                    continue;
                }
                reservationCount++;
                actualGuestCount += reservation.getGuestCount();
                if (reservation.getDepositStatus() != com.dada.eventmanagement.common.enums.DepositStatus.PAID) {
                    pendingDepositCount++;
                    BigDecimal paidDeposit = MathUtils.money(reservation.getDepositAmount());
                    BigDecimal remainingDeposit = minimumDepositAmount.subtract(paidDeposit);
                    if (remainingDeposit.compareTo(BigDecimal.ZERO) > 0) {
                        pendingDepositAmount = pendingDepositAmount.add(remainingDeposit);
                    }
                }
            }

            BigDecimal income = sum(transactions, FinancialTransactionType.INCOME);
            BigDecimal expense = sum(transactions, FinancialTransactionType.EXPENSE);
            rows.add(new EventProfitReportRowResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getEventDate(),
                    event.getStartTime() == null ? null : event.getStartTime().toString(),
                    event.getEndTime() == null ? null : event.getEndTime().toString(),
                    event.getVenueName(),
                    event.getExpectedGuestCount(),
                    reservationCount,
                    pendingDepositCount,
                    MathUtils.money(pendingDepositAmount),
                    actualGuestCount,
                    actualGuestCount,
                    event.getStatus() == null ? null : event.getStatus().name(),
                    closingReportByEvent.containsKey(event.getId()) ? "FINALIZED" : null,
                    income,
                    expense,
                    income.subtract(expense)
            ));
        }
        rows.sort(Comparator.comparing(EventProfitReportRowResponse::eventDate));
        return rows;
    }

    private void validateParentCategory(Long companyId, Long parentId) {
        if (parentId != null) {
            findCategory(parentId, companyId);
        }
    }

    private FinanceDocument findDocument(Long id, Long companyId) {
        return financeDocumentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Finance document not found"));
    }

    private FinancialCategory validateDocumentCategory(Long companyId, Long categoryId, FinanceDocumentType documentType) {
        FinancialCategory category = findCategory(categoryId, companyId);
        if (category.getCategoryType() != documentType.transactionType().categoryType()) {
            throw new BadRequestException("Document type must match category type");
        }
        if (category.getScope() == FinancialScope.EVENT) {
            throw new BadRequestException("Event-only categories cannot be used for general finance documents");
        }
        return category;
    }

    private FinanceDocument buildDocument(
            Long companyId,
            FinanceDocumentType documentType,
            Long categoryId,
            Long contactId,
            Long eventId,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal amount,
            String description,
            OperationContext operationContext
    ) {
        FinancialCategory category = validateDocumentCategory(companyId, categoryId, documentType);
        Contact contact = contactId == null ? null : contactRepository.findByIdAndCompanyIdAndIsActiveTrue(contactId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        Event event = eventId == null ? null : eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        FinanceDocument document = new FinanceDocument();
        document.setCompanyId(companyId);
        document.setDocumentType(documentType);
        document.setDocumentScope(FinancialScope.COMPANY);
        document.setCategoryId(category.getId());
        document.setContactId(contact == null ? null : contact.getId());
        document.setEventId(event == null ? null : event.getId());
        document.setIssueDate(issueDate);
        document.setDueDate(dueDate);
        document.setAmount(amount);
        document.setDescription(blankToNull(description));
        document.setStatus(FinanceDocumentStatus.OPEN);
        document.setOperationGroup(category.getOperationGroup());
        document.setOperationContext(operationContext == null ? OperationContext.COMPANY : operationContext);
        document.setCreatedByUserId(SecurityUtils.currentUser().getId());
        return document;
    }

    private FinanceDocument validateContactDocument(Long contactId, Long documentId, FinanceDocumentType expectedType) {
        Long companyId = SecurityUtils.currentCompanyId();
        FinanceDocument document = findDocument(documentId, companyId);
        if (document.getContactId() == null || !document.getContactId().equals(contactId)) {
            throw new BadRequestException("Document does not belong to the selected contact");
        }
        if (document.getDocumentType() != expectedType) {
            throw new BadRequestException("Document type does not match movement type");
        }
        if (document.getStatus() == FinanceDocumentStatus.VOIDED) {
            throw new BadRequestException("Voided document cannot accept settlements");
        }
        if (document.getStatus() == FinanceDocumentStatus.SETTLED) {
            throw new BadRequestException("Document is already settled");
        }
        return document;
    }

    private FinanceDocumentSettlement createSettlement(
            FinanceDocument document,
            FinanceDocumentSettlementRequest request,
            FinancialTransaction existingTransaction
    ) {
        Long companyId = document.getCompanyId();
        if (document.getStatus() == FinanceDocumentStatus.VOIDED) {
            throw new BadRequestException("Voided document cannot accept settlements");
        }
        if (document.getStatus() == FinanceDocumentStatus.SETTLED) {
            throw new BadRequestException("Document is already settled");
        }
        BigDecimal settledAmount = activeSettledAmount(companyId, List.of(document)).getOrDefault(document.getId(), BigDecimal.ZERO);
        BigDecimal remaining = document.getAmount().subtract(settledAmount);
        if (request.amount().compareTo(remaining) > 0) {
            throw new BadRequestException("Settlement amount cannot exceed remaining amount");
        }

        FinanceDocumentSettlement settlement = new FinanceDocumentSettlement();
        settlement.setCompanyId(companyId);
        settlement.setDocumentId(document.getId());
        settlement.setSettlementDate(request.settlementDate());
        settlement.setAmount(request.amount());
        settlement.setAccountId(resolveAccountId(companyId, request.accountId(), document.getDocumentType().transactionType()));
        settlement.setPaymentMethodId(resolvePaymentMethodId(companyId, request.paymentMethodId()));
        settlement.setNotes(blankToNull(request.notes()));
        settlement.setCreatedByUserId(SecurityUtils.currentUser().getId());
        settlement = financeDocumentSettlementRepository.save(settlement);

        FinancialTransaction transaction = existingTransaction;
        if (transaction == null) {
            transaction = createTransactionEntity(new FinancialTransactionRequest(
                    document.getEventId(),
                    settlement.getAccountId(),
                    settlement.getPaymentMethodId(),
                    document.getCategoryId(),
                    null,
                    document.getContactId(),
                    document.getDocumentType().transactionType(),
                    request.settlementDate(),
                    request.amount(),
                    null,
                    documentSettlementDescription(document, settlement),
                    OperationSource.FINANCE_DOCUMENT_SETTLEMENT,
                    document.getOperationContext(),
                    settlement.getId()
            ));
        }
        settlement.setFinancialTransactionId(transaction.getId());
        FinanceDocumentSettlement saved = financeDocumentSettlementRepository.save(settlement);
        recalculateDocumentStatus(document, settledAmount.add(request.amount()));
        financeDocumentRepository.save(document);
        return saved;
    }

    private Map<Long, BigDecimal> activeSettledAmount(Long companyId, List<FinanceDocument> documents) {
        if (documents.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = documents.stream().map(FinanceDocument::getId).toList();
        List<FinanceDocumentSettlement> settlements = financeDocumentSettlementRepository.findByCompanyIdAndDocumentIdInOrderBySettlementDateDescIdDesc(companyId, ids);
        List<Long> transactionIds = settlements.stream().map(FinanceDocumentSettlement::getFinancialTransactionId).filter(v -> v != null).distinct().toList();
        Map<Long, FinancialTransactionStatus> txStatuses = transactionIds.isEmpty()
                ? Map.of()
                : transactionRepository.findByCompanyIdAndIdIn(companyId, transactionIds).stream()
                .collect(Collectors.toMap(FinancialTransaction::getId, FinancialTransaction::getStatus));
        Map<Long, BigDecimal> totals = documents.stream().collect(Collectors.toMap(FinanceDocument::getId, row -> BigDecimal.ZERO));
        for (FinanceDocumentSettlement settlement : settlements) {
            FinancialTransactionStatus status = settlement.getFinancialTransactionId() == null
                    ? FinancialTransactionStatus.ACTIVE
                    : txStatuses.getOrDefault(settlement.getFinancialTransactionId(), FinancialTransactionStatus.VOIDED);
            if (status == FinancialTransactionStatus.ACTIVE) {
                totals.put(settlement.getDocumentId(), totals.getOrDefault(settlement.getDocumentId(), BigDecimal.ZERO).add(settlement.getAmount()));
            }
        }
        return totals;
    }

    private void recalculateDocumentStatus(FinanceDocument document) {
        BigDecimal settledAmount = activeSettledAmount(document.getCompanyId(), List.of(document)).getOrDefault(document.getId(), BigDecimal.ZERO);
        recalculateDocumentStatus(document, settledAmount);
        financeDocumentRepository.save(document);
    }

    private void recalculateDocumentStatus(FinanceDocument document, BigDecimal settledAmount) {
        if (document.getStatus() == FinanceDocumentStatus.VOIDED) {
            return;
        }
        BigDecimal remainingAmount = document.getAmount().subtract(settledAmount);
        if (remainingAmount.compareTo(document.getAmount()) == 0) {
            document.setStatus(FinanceDocumentStatus.OPEN);
        } else if (remainingAmount.compareTo(BigDecimal.ZERO) == 0) {
            document.setStatus(FinanceDocumentStatus.SETTLED);
        } else {
            document.setStatus(FinanceDocumentStatus.PARTIALLY_SETTLED);
        }
    }

    private String documentSettlementDescription(FinanceDocument document, FinanceDocumentSettlement settlement) {
        String base = document.getDescription() == null || document.getDescription().isBlank()
                ? (document.getDocumentType() == FinanceDocumentType.INCOME ? "Gelir tahsilati" : "Gider odemesi")
                : document.getDescription().trim();
        return base + " - Belge #" + document.getId() + " / Tahsilat #" + settlement.getId();
    }

    private List<FinancialTransaction> activeTransactions(Long companyId, DateRange range) {
        return transactionRepository.findByCompanyIdAndTransactionDateBetweenAndStatusOrderByTransactionDateDescIdDesc(
                companyId,
                range.start(),
                range.end(),
                FinancialTransactionStatus.ACTIVE
        );
    }

    private DateRange normalizeRange(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        return new DateRange(start, end);
    }

    private BigDecimal sum(List<FinancialTransaction> rows, FinancialTransactionType type) {
        return rows.stream()
                .filter(row -> row.getTransactionType() == type)
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private FinancialCategory findCategory(Long id, Long companyId) {
        return categoryRepository.findByIdAndCompanyIdAndIsActiveTrue(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial category not found"));
    }

    private RevenueChannel findRevenueChannel(Long id, Long companyId) {
        return revenueChannelRepository.findByIdAndCompanyIdAndIsActiveTrue(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Revenue channel not found"));
    }

    private Long resolveAccountId(Long companyId, Long accountId, FinancialTransactionType transactionType) {
        if (accountId != null) {
            return accountRepository.findByIdAndCompanyIdAndIsActiveTrue(accountId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Financial account not found"))
                    .getId();
        }
        AccountType type = transactionType == FinancialTransactionType.INCOME ? AccountType.CASH : AccountType.CASH;
        return accountRepository.findFirstByCompanyIdAndAccountTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, type)
                .orElseThrow(() -> new ResourceNotFoundException("Default cash account not found"))
                .getId();
    }

    private Long resolvePaymentMethodId(Long companyId, Long paymentMethodId) {
        if (paymentMethodId != null) {
            return paymentMethodRepository.findByIdAndCompanyIdAndIsActiveTrue(paymentMethodId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"))
                    .getId();
        }
        return paymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.CASH)
                .map(PaymentMethod::getId)
                .orElse(null);
    }

    private void updateAccountBalance(Long accountId, Long companyId, FinancialTransactionType type, BigDecimal amount) {
        FinancialAccount account = accountRepository.findByIdAndCompanyIdAndIsActiveTrue(accountId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found"));
        BigDecimal signedAmount = type == FinancialTransactionType.INCOME ? amount : amount.negate();
        account.setCurrentBalance(account.getCurrentBalance().add(signedAmount));
        accountRepository.save(account);
    }

    private void ensureDateIsOpen(Long companyId, LocalDate date) {
        dailyCashReportRepository.findByCompanyIdAndReportDate(companyId, date).ifPresent(report -> {
            if (report.getStatus() == DailyCashReportStatus.CLOSED) {
                throw new BadRequestException("This day is closed. Add a correction after reopening the day.");
            }
        });
    }

    private CashTotals cashTotals(Long companyId, LocalDate date) {
        BigDecimal totalIncome = transactionRepository.sumByDateAndType(companyId, date, FinancialTransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumByDateAndType(companyId, date, FinancialTransactionType.EXPENSE);
        Long cashPaymentMethodId = paymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.CASH)
                .map(PaymentMethod::getId)
                .orElse(null);
        Long cardPaymentMethodId = paymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.CARD)
                .map(PaymentMethod::getId)
                .orElse(null);
        Long bankPaymentMethodId = paymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.BANK)
                .map(PaymentMethod::getId)
                .orElse(null);
        Long currentPaymentMethodId = paymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.CURRENT_ACCOUNT)
                .map(PaymentMethod::getId)
                .orElse(null);

        return new CashTotals(
                totalIncome,
                totalExpense,
                sumByPayment(companyId, date, FinancialTransactionType.INCOME, cashPaymentMethodId),
                sumByPayment(companyId, date, FinancialTransactionType.INCOME, cardPaymentMethodId),
                sumByPayment(companyId, date, FinancialTransactionType.INCOME, bankPaymentMethodId),
                sumByPayment(companyId, date, FinancialTransactionType.INCOME, currentPaymentMethodId),
                sumByPayment(companyId, date, FinancialTransactionType.EXPENSE, cashPaymentMethodId)
        );
    }

    private BigDecimal sumByPayment(Long companyId, LocalDate date, FinancialTransactionType type, Long paymentMethodId) {
        if (paymentMethodId == null) {
            return BigDecimal.ZERO;
        }
        return transactionRepository.sumByDateTypeAndPaymentMethod(companyId, date, type, paymentMethodId);
    }

    private List<FinancialTransactionResponse> mapTransactions(List<FinancialTransaction> rows) {
        Long companyId = SecurityUtils.currentCompanyId();
        Map<Long, FinancialAccount> accounts = accountRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(FinancialAccount::getId, Function.identity()));
        Map<Long, PaymentMethod> paymentMethods = paymentMethodRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(PaymentMethod::getId, Function.identity()));
        Map<Long, FinancialCategory> categories = categoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream()
                .collect(Collectors.toMap(FinancialCategory::getId, Function.identity()));
        Map<Long, RevenueChannel> channels = revenueChannelRepository.findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(companyId).stream()
                .collect(Collectors.toMap(RevenueChannel::getId, Function.identity()));

        return rows.stream().map(t -> new FinancialTransactionResponse(
                t.getId(),
                t.getEventId(),
                t.getAccountId(),
                accounts.containsKey(t.getAccountId()) ? accounts.get(t.getAccountId()).getName() : null,
                t.getPaymentMethodId(),
                t.getPaymentMethodId() != null && paymentMethods.containsKey(t.getPaymentMethodId()) ? paymentMethods.get(t.getPaymentMethodId()).getName() : null,
                t.getCategoryId(),
                categories.containsKey(t.getCategoryId()) ? categories.get(t.getCategoryId()).getName() : null,
                t.getRevenueChannelId(),
                t.getRevenueChannelId() != null && channels.containsKey(t.getRevenueChannelId()) ? channels.get(t.getRevenueChannelId()).getName() : null,
                t.getContactId(),
                t.getTransactionType(),
                t.getTransactionDate(),
                t.getAmount(),
                t.getGuestCount(),
                t.getDescription(),
                t.getOperationSource(),
                t.getOperationContext(),
                t.getReferenceId(),
                t.getStatus(),
                t.getCreatedAt()
        )).toList();
    }

    private List<FinanceDocumentResponse> mapDocuments(List<FinanceDocument> rows) {
        Long companyId = SecurityUtils.currentCompanyId();
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> documentIds = rows.stream().map(FinanceDocument::getId).toList();
        List<FinanceDocumentSettlement> settlements = financeDocumentSettlementRepository.findByCompanyIdAndDocumentIdInOrderBySettlementDateDescIdDesc(companyId, documentIds);
        Map<Long, List<FinanceDocumentSettlement>> settlementMap = settlements.stream().collect(Collectors.groupingBy(FinanceDocumentSettlement::getDocumentId));
        List<Long> transactionIds = settlements.stream().map(FinanceDocumentSettlement::getFinancialTransactionId).filter(v -> v != null).distinct().toList();
        Map<Long, FinancialTransactionStatus> txStatuses = transactionIds.isEmpty()
                ? Map.of()
                : transactionRepository.findByCompanyIdAndIdIn(companyId, transactionIds).stream()
                .collect(Collectors.toMap(FinancialTransaction::getId, FinancialTransaction::getStatus));

        return rows.stream().map(document -> {
            List<FinanceDocumentSettlementResponse> settlementResponses = settlementMap.getOrDefault(document.getId(), List.of()).stream()
                    .map(settlement -> toResponse(settlement, settlement.getFinancialTransactionId() == null ? FinancialTransactionStatus.ACTIVE : txStatuses.getOrDefault(settlement.getFinancialTransactionId(), FinancialTransactionStatus.VOIDED)))
                    .toList();
            BigDecimal settledAmount = settlementResponses.stream()
                    .filter(settlement -> settlement.financialTransactionStatus() == FinancialTransactionStatus.ACTIVE)
                    .map(FinanceDocumentSettlementResponse::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate lastSettlementDate = settlementResponses.stream()
                    .filter(settlement -> settlement.financialTransactionStatus() == FinancialTransactionStatus.ACTIVE)
                    .map(FinanceDocumentSettlementResponse::settlementDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);
            return new FinanceDocumentResponse(
                    document.getId(),
                    document.getDocumentType(),
                    document.getDocumentScope(),
                    document.getCategoryId(),
                    document.getContactId(),
                    document.getEventId(),
                    document.getIssueDate(),
                    document.getDueDate(),
                    document.getAmount(),
                    settledAmount,
                    document.getAmount().subtract(settledAmount),
                    document.getDescription(),
                    document.getStatus(),
                    document.getOperationGroup(),
                    document.getOperationContext(),
                    lastSettlementDate,
                    document.getCreatedAt(),
                    settlementResponses
            );
        }).toList();
    }

    private FinanceDocumentSettlementResponse toResponse(FinanceDocumentSettlement settlement, FinancialTransactionStatus transactionStatus) {
        return new FinanceDocumentSettlementResponse(
                settlement.getId(),
                settlement.getDocumentId(),
                settlement.getFinancialTransactionId(),
                transactionStatus,
                settlement.getSettlementDate(),
                settlement.getAmount(),
                settlement.getAccountId(),
                settlement.getPaymentMethodId(),
                settlement.getNotes(),
                settlement.getCreatedAt()
        );
    }

    private OperationSource defaultOperationSource(FinancialTransactionRequest request) {
        if (request.contactId() != null) {
            return request.transactionType() == FinancialTransactionType.INCOME
                    ? OperationSource.CONTACT_COLLECTION
                    : OperationSource.CONTACT_PAYMENT;
        }
        if (request.eventId() != null) {
            return request.transactionType() == FinancialTransactionType.INCOME
                    ? OperationSource.EVENT_REVENUE
                    : OperationSource.COMPANY_EXPENSE;
        }
        return request.transactionType() == FinancialTransactionType.EXPENSE
                ? OperationSource.COMPANY_EXPENSE
                : OperationSource.MANUAL;
    }

    private OperationContext defaultOperationContext(FinancialTransactionRequest request) {
        if (request.contactId() != null) {
            return OperationContext.CONTACT;
        }
        if (request.eventId() != null) {
            return OperationContext.EVENT;
        }
        return OperationContext.COMPANY;
    }

    private FinancialAccountResponse toResponse(FinancialAccount account) {
        return new FinancialAccountResponse(account.getId(), account.getName(), account.getAccountType(), account.getOpeningBalance(), account.getCurrentBalance(), account.getIsDefault());
    }

    private FinancialAccount findAccount(Long id, Long companyId) {
        return accountRepository.findByIdAndCompanyIdAndIsActiveTrue(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Financial account not found"));
    }

    private void clearDefaultAccounts(Long companyId, Long exceptId) {
        List<FinancialAccount> accounts = accountRepository.findByCompanyIdAndIsDefaultTrueAndIsActiveTrue(companyId);
        for (FinancialAccount account : accounts) {
            if (exceptId == null || !exceptId.equals(account.getId())) {
                account.setIsDefault(false);
            }
        }
        accountRepository.saveAll(accounts);
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return new PaymentMethodResponse(method.getId(), method.getName(), method.getMethodType(), method.getIsDefault());
    }

    private FinancialCategoryResponse toResponse(FinancialCategory category) {
        return new FinancialCategoryResponse(category.getId(), category.getParentId(), category.getName(), category.getCategoryType(), category.getScope(), category.getOperationGroup(), category.getDescription(), category.getIsDefault());
    }

    private RevenueChannelResponse toResponse(RevenueChannel channel) {
        return new RevenueChannelResponse(channel.getId(), channel.getName(), channel.getChannelType(), channel.getSortOrder(), channel.getIsDefault());
    }

    private DailyCashReportResponse toResponse(DailyCashReport report) {
        return new DailyCashReportResponse(
                report.getId(),
                report.getReportDate(),
                report.getOpeningCash(),
                report.getManagementCashIn(),
                report.getCashIncome(),
                report.getCardIncome(),
                report.getBankIncome(),
                report.getCurrentAccountIncome(),
                report.getCashExpense(),
                report.getTotalIncome(),
                report.getTotalExpense(),
                report.getExpectedCash(),
                report.getActualCash(),
                report.getCashDifference(),
                report.getManagementCashOut(),
                report.getNotes(),
                report.getStatus()
        );
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void markEventsCompleted(Long companyId, LocalDate reportDate) {
        List<Event> events = eventRepository.findByCompanyIdAndEventDateAndIsDeletedFalse(companyId, reportDate);
        for (Event event : events) {
            if (event.getStatus() != EventStatus.CANCELLED && event.getStatus() != EventStatus.COMPLETED) {
                event.setStatus(EventStatus.COMPLETED);
            }
        }
        eventRepository.saveAll(events);
    }

    private record CashTotals(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal cashIncome,
            BigDecimal cardIncome,
            BigDecimal bankIncome,
            BigDecimal currentAccountIncome,
            BigDecimal cashExpense
    ) {
    }

    public record ContactOpenAmounts(
            BigDecimal openReceivableAmount,
            BigDecimal openPayableAmount
    ) {
    }

    private record DateRange(LocalDate start, LocalDate end) {
    }
}



