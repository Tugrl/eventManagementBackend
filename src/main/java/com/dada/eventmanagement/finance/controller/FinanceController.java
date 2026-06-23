package com.dada.eventmanagement.finance.controller;

import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.finance.dto.*;
import com.dada.eventmanagement.finance.service.FinanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    @GetMapping("/accounts")
    public ApiResponse<?> accounts() {
        return ApiResponse.ok("Financial accounts listed", service.accounts());
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createAccount(@Valid @RequestBody FinancialAccountRequest request) {
        return ApiResponse.ok("Financial account created", service.createAccount(request));
    }

    @PutMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateAccount(@PathVariable Long id, @Valid @RequestBody FinancialAccountRequest request) {
        return ApiResponse.ok("Financial account updated", service.updateAccount(id, request));
    }

    @DeleteMapping("/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteAccount(@PathVariable Long id) {
        service.deleteAccount(id);
        return ApiResponse.ok("Financial account deleted");
    }

    @GetMapping("/payment-methods")
    public ApiResponse<?> paymentMethods() {
        return ApiResponse.ok("Payment methods listed", service.paymentMethods());
    }

    @GetMapping("/categories")
    public ApiResponse<?> categories(@RequestParam(required = false) FinancialCategoryType type) {
        return ApiResponse.ok("Financial categories listed", service.categories(type));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createCategory(@Valid @RequestBody FinancialCategoryRequest request) {
        return ApiResponse.ok("Financial category created", service.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateCategory(@PathVariable Long id, @Valid @RequestBody FinancialCategoryRequest request) {
        return ApiResponse.ok("Financial category updated", service.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ApiResponse.ok("Financial category deleted");
    }

    @GetMapping("/revenue-channels")
    public ApiResponse<?> revenueChannels() {
        return ApiResponse.ok("Revenue channels listed", service.revenueChannels());
    }

    @PostMapping("/revenue-channels")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> createRevenueChannel(@Valid @RequestBody RevenueChannelRequest request) {
        return ApiResponse.ok("Revenue channel created", service.createRevenueChannel(request));
    }

    @PutMapping("/revenue-channels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> updateRevenueChannel(@PathVariable Long id, @Valid @RequestBody RevenueChannelRequest request) {
        return ApiResponse.ok("Revenue channel updated", service.updateRevenueChannel(id, request));
    }

    @DeleteMapping("/revenue-channels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> deleteRevenueChannel(@PathVariable Long id) {
        service.deleteRevenueChannel(id);
        return ApiResponse.ok("Revenue channel deleted");
    }

    @GetMapping("/transactions")
    public ApiResponse<?> transactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) FinancialTransactionType transactionType,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) OperationSource operationSource,
            @RequestParam(required = false) FinancialTransactionStatus status
    ) {
        return ApiResponse.ok("Financial transactions listed", service.transactions(
                startDate, endDate, transactionType, accountId, categoryId, eventId, contactId, operationSource, status
        ));
    }

    @GetMapping("/documents")
    public ApiResponse<?> documents(
            @RequestParam(required = false) FinanceDocumentType documentType,
            @RequestParam(required = false) FinanceDocumentStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long contactId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Finance documents listed", service.documents(documentType, status, categoryId, contactId, startDate, endDate));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<?> documentDetail(@PathVariable Long id) {
        return ApiResponse.ok("Finance document loaded", service.documentDetail(id));
    }

    @PostMapping("/documents")
    public ApiResponse<?> createDocument(@Valid @RequestBody FinanceDocumentRequest request) {
        return ApiResponse.ok("Finance document created", service.createDocument(request));
    }

    @PutMapping("/documents/{id}")
    public ApiResponse<?> updateDocument(@PathVariable Long id, @Valid @RequestBody FinanceDocumentRequest request) {
        return ApiResponse.ok("Finance document updated", service.updateDocument(id, request));
    }

    @PostMapping("/documents/{id}/settlements")
    public ApiResponse<?> createDocumentSettlement(@PathVariable Long id, @Valid @RequestBody FinanceDocumentSettlementRequest request) {
        return ApiResponse.ok("Finance document settlement created", service.createDocumentSettlement(id, request));
    }

    @PostMapping("/documents/{id}/void")
    public ApiResponse<?> voidDocument(@PathVariable Long id, @Valid @RequestBody VoidTransactionRequest request) {
        service.voidDocument(id, request);
        return ApiResponse.ok("Finance document voided");
    }

    @GetMapping("/dashboard-summary")
    public ApiResponse<?> dashboardSummary() {
        return ApiResponse.ok("Finance dashboard summary calculated", service.dashboardSummary());
    }

    @PostMapping("/transactions")
    public ApiResponse<?> createTransaction(@Valid @RequestBody FinancialTransactionRequest request) {
        return ApiResponse.ok("Financial transaction created", service.createTransaction(request));
    }

    @PostMapping("/transactions/{id}/void")
    public ApiResponse<?> voidTransaction(@PathVariable Long id, @Valid @RequestBody VoidTransactionRequest request) {
        service.voidTransaction(id, request);
        return ApiResponse.ok("Financial transaction voided");
    }

    @GetMapping("/daily-cash/summary")
    public ApiResponse<?> dailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.ok("Daily cash summary calculated", service.dailySummary(date));
    }

    @PostMapping("/daily-cash/close")
    public ApiResponse<?> closeDailyCash(@Valid @RequestBody DailyCashCloseRequest request) {
        return ApiResponse.ok("Daily cash closed", service.closeDailyCash(request));
    }

    @PostMapping("/daily-cash/reports/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> reopenDailyCash(@PathVariable Long id) {
        return ApiResponse.ok("Daily cash reopened", service.reopenDailyCash(id));
    }

    @GetMapping("/daily-cash/reports")
    public ApiResponse<?> cashReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Daily cash reports listed", service.cashReports(startDate, endDate));
    }

    @GetMapping("/events/{eventId}/profit-summary")
    public ApiResponse<?> eventProfit(@PathVariable Long eventId) {
        return ApiResponse.ok("Event profit summary calculated", service.eventProfit(eventId));
    }

    @GetMapping("/reports/overview")
    public ApiResponse<?> overviewReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Finance overview report calculated", service.overviewReport(startDate, endDate));
    }

    @GetMapping("/reports/categories")
    public ApiResponse<?> categoryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Category report calculated", service.categoryReport(startDate, endDate));
    }

    @GetMapping("/reports/revenue-channels")
    public ApiResponse<?> revenueChannelReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Revenue channel report calculated", service.revenueChannelReport(startDate, endDate));
    }

    @GetMapping("/reports/events")
    public ApiResponse<?> eventProfitReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ApiResponse.ok("Event profit report calculated", service.eventProfitReport(startDate, endDate));
    }
}
