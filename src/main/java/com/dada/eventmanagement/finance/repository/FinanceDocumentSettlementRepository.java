package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.finance.entity.FinanceDocumentSettlement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceDocumentSettlementRepository extends JpaRepository<FinanceDocumentSettlement, Long> {
    List<FinanceDocumentSettlement> findByCompanyIdAndDocumentIdOrderBySettlementDateDescIdDesc(Long companyId, Long documentId);
    List<FinanceDocumentSettlement> findByCompanyIdAndDocumentIdInOrderBySettlementDateDescIdDesc(Long companyId, List<Long> documentIds);
    Optional<FinanceDocumentSettlement> findByIdAndCompanyId(Long id, Long companyId);
    List<FinanceDocumentSettlement> findByCompanyIdAndFinancialTransactionId(Long companyId, Long financialTransactionId);
}
