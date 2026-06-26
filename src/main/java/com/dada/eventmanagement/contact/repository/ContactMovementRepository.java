package com.dada.eventmanagement.contact.repository;

import com.dada.eventmanagement.contact.entity.ContactMovement;
import com.dada.eventmanagement.common.enums.ContactMovementType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContactMovementRepository extends JpaRepository<ContactMovement, Long> {
    List<ContactMovement> findByCompanyIdAndContactIdOrderByMovementDateDescIdDesc(Long companyId, Long contactId);
    Optional<ContactMovement> findByIdAndCompanyIdAndContactId(Long id, Long companyId, Long contactId);
    List<ContactMovement> findByCompanyIdAndContactIdInAndMovementType(Long companyId, List<Long> contactIds, ContactMovementType movementType);
    List<ContactMovement> findByCompanyIdAndContactIdInAndDocumentIdIsNullAndSettlementIdIsNull(Long companyId, List<Long> contactIds);

    @Query("""
            select m.contactId, max(m.movementDate)
            from ContactMovement m
            where m.companyId = :companyId
              and m.contactId in :contactIds
            group by m.contactId
            """)
    List<Object[]> findLastMovementDates(Long companyId, List<Long> contactIds);
}
