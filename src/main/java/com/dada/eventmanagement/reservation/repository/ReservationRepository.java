package com.dada.eventmanagement.reservation.repository;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.ReservationStatus;
import com.dada.eventmanagement.reservation.entity.Reservation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Reservation> findByCompanyIdAndEventIdOrderByCreatedAtDesc(Long companyId, Long eventId);
    List<Reservation> findByCompanyIdAndEventIdIn(Long companyId, Collection<Long> eventIds);
    List<Reservation> findByCompanyIdAndReservationStatusOrderByCreatedAtDesc(Long companyId, ReservationStatus reservationStatus);
    List<Reservation> findByCompanyIdAndEventIdAndReservationStatusOrderByCreatedAtDesc(Long companyId, Long eventId, ReservationStatus reservationStatus);
    Optional<Reservation> findByIdAndCompanyId(Long id, Long companyId);
    long countByCompanyIdAndReservationCodeStartingWith(Long companyId, String codePrefix);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndDepositStatus(Long companyId, DepositStatus depositStatus);
    long countByCompanyIdAndEventId(Long companyId, Long eventId);
    long countByCompanyIdAndEventIdAndReservationStatus(Long companyId, Long eventId, ReservationStatus reservationStatus);
    long countByCompanyIdAndEventIdAndReservationStatusAndDepositStatus(Long companyId, Long eventId, ReservationStatus reservationStatus, DepositStatus depositStatus);

    @Query("""
            select coalesce(sum(r.guestCount), 0)
            from Reservation r
            where r.companyId = :companyId
              and r.eventId = :eventId
              and r.reservationStatus = :reservationStatus
              and r.depositStatus = :depositStatus
            """)
    long sumGuestCountByCompanyAndEventAndStatus(Long companyId, Long eventId, ReservationStatus reservationStatus, DepositStatus depositStatus);
}
