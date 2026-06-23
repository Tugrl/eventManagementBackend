package com.dada.eventmanagement.staff.repository;

import com.dada.eventmanagement.staff.entity.EventStaffAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStaffAssignmentRepository extends JpaRepository<EventStaffAssignment, Long> {
    List<EventStaffAssignment> findByCompanyIdAndEventIdOrderByCreatedAtAsc(Long companyId, Long eventId);
    long countByCompanyIdAndEventId(Long companyId, Long eventId);
    void deleteByCompanyIdAndEventId(Long companyId, Long eventId);
}
