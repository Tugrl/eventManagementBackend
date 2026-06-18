package com.dada.eventmanagement.report.service;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.EventStatus;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.report.dto.DashboardResponse;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final EventCostRepository eventCostRepository;

    public DashboardService(EventRepository eventRepository, ReservationRepository reservationRepository, EventCostRepository eventCostRepository) {
        this.eventRepository = eventRepository;
        this.reservationRepository = reservationRepository;
        this.eventCostRepository = eventCostRepository;
    }

    public DashboardResponse getDashboard() {
        Long companyId = SecurityUtils.currentCompanyId();
        long totalEvents = eventRepository.countByCompanyIdAndIsDeletedFalse(companyId);
        long upcomingEvents = eventRepository.countByCompanyIdAndEventDateGreaterThanEqualAndIsDeletedFalse(companyId, LocalDate.now());
        long completedEvents = eventRepository.countByCompanyIdAndStatusAndIsDeletedFalse(companyId, EventStatus.COMPLETED);
        long totalReservations = reservationRepository.countByCompanyId(companyId);
        long totalPaidDeposits = reservationRepository.countByCompanyIdAndDepositStatus(companyId, DepositStatus.PAID);

        BigDecimal totalEstimatedCost = MathUtils.money(eventCostRepository.sumEstimatedCostByCompany(companyId));
        BigDecimal totalEstimatedRevenue = MathUtils.money(eventRepository.sumEstimatedRevenue(companyId));
        BigDecimal totalEstimatedProfit = MathUtils.money(totalEstimatedRevenue.subtract(totalEstimatedCost));

        List<DashboardResponse.NearestEventDto> nearestEvents = eventRepository
                .findTop5ByCompanyIdAndEventDateGreaterThanEqualAndIsDeletedFalseOrderByEventDateAsc(companyId, LocalDate.now())
                .stream()
                .map(e -> new DashboardResponse.NearestEventDto(e.getId(), e.getTitle(), e.getEventDate(), e.getVenueName()))
                .toList();

        return new DashboardResponse(
                totalEvents,
                upcomingEvents,
                completedEvents,
                totalReservations,
                totalPaidDeposits,
                totalEstimatedRevenue,
                totalEstimatedCost,
                totalEstimatedProfit,
                nearestEvents
        );
    }
}
