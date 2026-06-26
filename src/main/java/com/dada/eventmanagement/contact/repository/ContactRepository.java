package com.dada.eventmanagement.contact.repository;

import com.dada.eventmanagement.common.enums.ContactType;
import com.dada.eventmanagement.contact.entity.Contact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByCompanyIdAndIsActiveTrueOrderByNameAsc(Long companyId);
    List<Contact> findByCompanyIdAndContactTypeAndIsActiveTrueOrderByNameAsc(Long companyId, ContactType contactType);
    List<Contact> findByCompanyIdAndIdInAndIsActiveTrue(Long companyId, List<Long> ids);
    Optional<Contact> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
}
