package com.dada.eventmanagement.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ClosingReportResponse(
        Long id,
        Long eventId,
        Integer actualGuestCount,
        BigDecimal finalTicketPrice,
        BigDecimal grossTicketPotential,
        BigDecimal collectedPaymentAmount,
        BigDecimal remainingReceivableAmount,
        BigDecimal actualTicketRevenue,
        BigDecimal actualDepositAmount,
        BigDecimal actualDoorPaymentAmount,
        BigDecimal actualExtraSalesAmount,
        BigDecimal actualTotalRevenue,
        BigDecimal actualTotalCost,
        BigDecimal collectedRevenueAmount,
        BigDecimal openReceivableAmount,
        BigDecimal totalAccruedRevenueAmount,
        BigDecimal paidCostAmount,
        BigDecimal openPayableAmount,
        BigDecimal totalAccruedCostAmount,
        BigDecimal cashBasisProfit,
        BigDecimal accrualBasisProfit,
        BigDecimal servicePayoutTotal,
        BigDecimal servicePayoutPaidAmount,
        BigDecimal servicePayoutRemainingAmount,
        BigDecimal servicePayoutDocumentBackedAmount,
        Integer servicePayoutOpenDocumentCount,
        Integer servicePayoutSettledDocumentCount,
        BigDecimal actualProfit,
        BigDecimal estimatedTotalRevenue,
        BigDecimal estimatedTotalCost,
        BigDecimal estimatedProfit,
        BigDecimal profitDifference,
        String notes,
        LocalDateTime updatedAt
) {
}
