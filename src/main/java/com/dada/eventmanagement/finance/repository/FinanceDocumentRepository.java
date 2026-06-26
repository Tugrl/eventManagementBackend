package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.finance.entity.FinanceDocument;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinanceDocumentRepository extends JpaRepository<FinanceDocument, Long> {
    Optional<FinanceDocument> findByIdAndCompanyId(Long id, Long companyId);

    @Query("""
            select d from FinanceDocument d
            where d.companyId = :companyId
              and (:documentType is null or d.documentType = :documentType)
              and (:status is null or d.status = :status)
              and (:categoryId is null or d.categoryId = :categoryId)
              and (:contactId is null or d.contactId = :contactId)
              and d.issueDate between :startDate and :endDate
            order by d.issueDate desc, d.id desc
            """)
    List<FinanceDocument> search(
            Long companyId,
            FinanceDocumentType documentType,
            FinanceDocumentStatus status,
            Long categoryId,
            Long contactId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<FinanceDocument> findByCompanyIdAndStatusNotOrderByIssueDateDescIdDesc(Long companyId, FinanceDocumentStatus status);
    List<FinanceDocument> findByCompanyIdAndContactIdInAndStatusNot(Long companyId, List<Long> contactIds, FinanceDocumentStatus status);
    List<FinanceDocument> findByCompanyIdAndEventIdAndDocumentTypeAndStatusNotOrderByIssueDateDescIdDesc(
            Long companyId,
            Long eventId,
            FinanceDocumentType documentType,
            FinanceDocumentStatus status
    );
}
