package com.dada.eventmanagement.contact.repository;

import com.dada.eventmanagement.contact.entity.ContactMovement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMovementRepository extends JpaRepository<ContactMovement, Long> {
    List<ContactMovement> findByCompanyIdAndContactIdOrderByMovementDateDescIdDesc(Long companyId, Long contactId);
    Optional<ContactMovement> findByIdAndCompanyIdAndContactId(Long id, Long companyId, Long contactId);
}
