package com.dada.eventmanagement.finance.service;

import com.dada.eventmanagement.common.enums.AccountType;
import com.dada.eventmanagement.common.enums.DailyCashReportStatus;
import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.PaymentMethodType;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.finance.dto.*;
import com.dada.eventmanagement.finance.entity.*;
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

    public FinanceService(
            FinancialAccountRepository accountRepository,
            PaymentMethodRepository paymentMethodRepository,
            FinancialCategoryRepository categoryRepository,
            RevenueChannelRepository revenueChannelRepository,
            FinancialTransactionRepository transactionRepository,
            DailyCashReportRepository dailyCashReportRepository,
            EventRepository eventRepository
    ) {
        this.accountRepository = accountRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.categoryRepository = categoryRepository;
        this.revenueChannelRepository = revenueChannelRepository;
        this.transactionRepository = transactionRepository;
        this.dailyCashReportRepository = dailyCashReportRepository;
        this.eventRepository = eventRepository;
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

    public List<FinancialTransactionResponse> transactions(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now() : startDate;
        LocalDate end = endDate == null ? start : endDate;
        if (end.isBefore(start)) {
            throw new BadRequestException("End date cannot be before start date");
        }
        return mapTransactions(transactionRepository.findByCompanyIdAndTransactionDateBetweenAndStatusOrderByTransactionDateDescIdDesc(
                companyId,
                start,
                end,
                FinancialTransactionStatus.ACTIVE
        ));
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
        tx.setCreatedByUserId(SecurityUtils.currentUser().getId());
        tx.setStatus(FinancialTransactionStatus.ACTIVE);
        updateAccountBalance(accountId, companyId, request.transactionType(), request.amount());
        return transactionRepository.save(tx);
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
        Map<Long, com.dada.eventmanagement.event.entity.Event> eventMap = eventRepository
                .findByCompanyIdAndIsDeletedFalseOrderByEventDateAsc(companyId)
                .stream()
                .collect(Collectors.toMap(com.dada.eventmanagement.event.entity.Event::getId, Function.identity()));
        Map<Long, List<FinancialTransaction>> grouped = activeTransactions(companyId, range).stream()
                .filter(row -> row.getEventId() != null)
                .collect(Collectors.groupingBy(FinancialTransaction::getEventId));

        List<EventProfitReportRowResponse> rows = new ArrayList<>();
        grouped.forEach((eventId, transactions) -> {
            com.dada.eventmanagement.event.entity.Event event = eventMap.get(eventId);
            if (event == null) {
                return;
            }
            BigDecimal income = sum(transactions, FinancialTransactionType.INCOME);
            BigDecimal expense = sum(transactions, FinancialTransactionType.EXPENSE);
            rows.add(new EventProfitReportRowResponse(eventId, event.getTitle(), event.getEventDate(), income, expense, income.subtract(expense)));
        });
        rows.sort(Comparator.comparing(EventProfitReportRowResponse::eventDate));
        return rows;
    }

    private void validateParentCategory(Long companyId, Long parentId) {
        if (parentId != null) {
            findCategory(parentId, companyId);
        }
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
                t.getStatus(),
                t.getCreatedAt()
        )).toList();
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
        return new FinancialCategoryResponse(category.getId(), category.getParentId(), category.getName(), category.getCategoryType(), category.getScope(), category.getDescription(), category.getIsDefault());
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

    private record DateRange(LocalDate start, LocalDate end) {
    }
}
