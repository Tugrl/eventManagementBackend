package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.common.enums.FinancialTransactionStatus;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    List<FinancialTransaction> findByCompanyIdAndTransactionDateBetweenAndStatusOrderByTransactionDateDescIdDesc(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            FinancialTransactionStatus status
    );

    Optional<FinancialTransaction> findByIdAndCompanyId(Long id, Long companyId);
    List<FinancialTransaction> findByCompanyIdAndIdIn(Long companyId, List<Long> ids);
    Optional<FinancialTransaction> findFirstByCompanyIdAndOperationSourceAndReferenceIdAndStatus(
            Long companyId,
            OperationSource operationSource,
            Long referenceId,
            FinancialTransactionStatus status
    );

    @Query("""
            select t from FinancialTransaction t
            where t.companyId = :companyId
              and t.transactionDate between :startDate and :endDate
              and (:transactionType is null or t.transactionType = :transactionType)
              and (:accountId is null or t.accountId = :accountId)
              and (:categoryId is null or t.categoryId = :categoryId)
              and (:eventId is null or t.eventId = :eventId)
              and (:contactId is null or t.contactId = :contactId)
              and (:operationSource is null or t.operationSource = :operationSource)
              and (:status is null or t.status = :status)
            order by t.transactionDate desc, t.id desc
            """)
    List<FinancialTransaction> search(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            FinancialTransactionType transactionType,
            Long accountId,
            Long categoryId,
            Long eventId,
            Long contactId,
            OperationSource operationSource,
            FinancialTransactionStatus status
    );

    List<FinancialTransaction> findByCompanyIdAndEventIdAndTransactionTypeAndStatusOrderByTransactionDateDescIdDesc(
            Long companyId,
            Long eventId,
            FinancialTransactionType transactionType,
            FinancialTransactionStatus status
    );

    @Query("""
            select coalesce(sum(t.amount), 0)
            from FinancialTransaction t
            where t.companyId = :companyId
              and t.transactionDate = :date
              and t.transactionType = :type
              and t.status = 'ACTIVE'
            """)
    BigDecimal sumByDateAndType(Long companyId, LocalDate date, FinancialTransactionType type);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from FinancialTransaction t
            where t.companyId = :companyId
              and t.transactionDate = :date
              and t.transactionType = :type
              and t.paymentMethodId = :paymentMethodId
              and t.status = 'ACTIVE'
            """)
    BigDecimal sumByDateTypeAndPaymentMethod(Long companyId, LocalDate date, FinancialTransactionType type, Long paymentMethodId);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from FinancialTransaction t
            where t.companyId = :companyId
              and t.eventId = :eventId
              and t.transactionType = :type
              and t.status = 'ACTIVE'
            """)
    BigDecimal sumByEventAndType(Long companyId, Long eventId, FinancialTransactionType type);
}
