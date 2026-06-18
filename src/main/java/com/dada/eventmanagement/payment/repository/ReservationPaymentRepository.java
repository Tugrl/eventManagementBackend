package com.dada.eventmanagement.payment.repository;

import com.dada.eventmanagement.payment.entity.ReservationPayment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {
    List<ReservationPayment> findByCompanyIdAndReservationIdOrderByCreatedAtDesc(Long companyId, Long reservationId);
    Optional<ReservationPayment> findByIdAndCompanyId(Long id, Long companyId);

    @Query("select coalesce(sum(p.amount), 0) from ReservationPayment p where p.companyId = :companyId and p.reservationId = :reservationId")
    BigDecimal sumByCompanyAndReservationId(Long companyId, Long reservationId);

    @Query("select coalesce(sum(p.amount), 0) from ReservationPayment p where p.companyId = :companyId and p.eventId = :eventId")
    BigDecimal sumByCompanyAndEventId(Long companyId, Long eventId);

    @Query("select coalesce(sum(p.amount), 0) from ReservationPayment p where p.companyId = :companyId")
    BigDecimal sumByCompanyId(Long companyId);
}
