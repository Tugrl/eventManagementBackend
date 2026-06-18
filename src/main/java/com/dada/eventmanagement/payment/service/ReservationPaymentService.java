package com.dada.eventmanagement.payment.service;

import com.dada.eventmanagement.common.enums.DepositStatus;
import com.dada.eventmanagement.common.enums.FinancialCategoryType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.PaymentMethodType;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.MathUtils;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.service.EventService;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.VoidTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialCategory;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.entity.PaymentMethod;
import com.dada.eventmanagement.finance.entity.RevenueChannel;
import com.dada.eventmanagement.finance.repository.FinancialCategoryRepository;
import com.dada.eventmanagement.finance.repository.PaymentMethodRepository;
import com.dada.eventmanagement.finance.repository.RevenueChannelRepository;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.payment.dto.ReservationPaymentRequest;
import com.dada.eventmanagement.payment.dto.ReservationPaymentResponse;
import com.dada.eventmanagement.payment.entity.ReservationPayment;
import com.dada.eventmanagement.payment.repository.ReservationPaymentRepository;
import com.dada.eventmanagement.reservation.entity.Reservation;
import com.dada.eventmanagement.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationPaymentService {
    private final ReservationPaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final EventService eventService;
    private final FinanceService financeService;
    private final FinancialCategoryRepository categoryRepository;
    private final PaymentMethodRepository financePaymentMethodRepository;
    private final RevenueChannelRepository revenueChannelRepository;

    public ReservationPaymentService(
            ReservationPaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            EventService eventService,
            FinanceService financeService,
            FinancialCategoryRepository categoryRepository,
            PaymentMethodRepository financePaymentMethodRepository,
            RevenueChannelRepository revenueChannelRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.eventService = eventService;
        this.financeService = financeService;
        this.categoryRepository = categoryRepository;
        this.financePaymentMethodRepository = financePaymentMethodRepository;
        this.revenueChannelRepository = revenueChannelRepository;
    }

    public List<ReservationPaymentResponse> list(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        return paymentRepository.findByCompanyIdAndReservationIdOrderByCreatedAtDesc(reservation.getCompanyId(), reservationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReservationPaymentResponse create(Long reservationId, ReservationPaymentRequest request) {
        Reservation reservation = findReservation(reservationId);
        ReservationPayment payment = new ReservationPayment();
        payment.setCompanyId(reservation.getCompanyId());
        payment.setReservationId(reservationId);
        payment.setEventId(reservation.getEventId());
        payment.setAmount(MathUtils.money(request.amount()));
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setBankReference(request.bankReference());
        payment.setDescription(request.description());
        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                reservation.getEventId(),
                null,
                resolveFinancePaymentMethodId(reservation.getCompanyId(), request.paymentMethod()),
                resolveIncomeCategoryId(reservation.getCompanyId()),
                resolveRevenueChannelId(reservation.getCompanyId()),
                null,
                FinancialTransactionType.INCOME,
                request.paymentDate(),
                MathUtils.money(request.amount()),
                reservation.getGuestCount(),
                paymentDescription(reservation, request)
        ));
        payment.setFinancialTransactionId(tx.getId());
        ReservationPayment saved = paymentRepository.save(payment);
        recalculateDepositStatus(reservation);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        ReservationPayment payment = paymentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation payment not found"));
        Reservation reservation = findReservation(payment.getReservationId());
        if (payment.getFinancialTransactionId() != null) {
            financeService.voidTransaction(payment.getFinancialTransactionId(), new VoidTransactionRequest("Rezervasyon ödemesi silindi"));
        }
        paymentRepository.delete(payment);
        recalculateDepositStatus(reservation);
    }

    private Reservation findReservation(Long reservationId) {
        Long companyId = SecurityUtils.currentCompanyId();
        return reservationRepository.findByIdAndCompanyId(reservationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    private void recalculateDepositStatus(Reservation reservation) {
        BigDecimal totalPaid = MathUtils.money(paymentRepository.sumByCompanyAndReservationId(reservation.getCompanyId(), reservation.getId()));
        Event event = eventService.findEvent(reservation.getEventId());
        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            reservation.setDepositStatus(DepositStatus.PENDING);
        } else if (Boolean.FALSE.equals(event.getDepositRequired())) {
            reservation.setDepositStatus(DepositStatus.PAID);
        } else {
            BigDecimal minimum = MathUtils.money(event.getMinimumDepositAmount());
            if (totalPaid.compareTo(minimum) >= 0) {
                reservation.setDepositStatus(DepositStatus.PAID);
            } else {
                reservation.setDepositStatus(DepositStatus.PARTIALLY_PAID);
            }
        }
        reservationRepository.save(reservation);
    }

    private ReservationPaymentResponse toResponse(ReservationPayment payment) {
        return new ReservationPaymentResponse(
                payment.getId(),
                payment.getReservationId(),
                payment.getEventId(),
                MathUtils.money(payment.getAmount()),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getBankReference(),
                payment.getDescription(),
                payment.getFinancialTransactionId(),
                payment.getCreatedAt()
        );
    }

    private Long resolveIncomeCategoryId(Long companyId) {
        List<FinancialCategory> categories = categoryRepository.findByCompanyIdAndCategoryTypeAndIsActiveTrueOrderByNameAsc(companyId, FinancialCategoryType.INCOME);
        return categories.stream()
                .filter(category -> category.getName().equalsIgnoreCase("Etkinlik Geliri") || category.getName().equalsIgnoreCase("Bilet Satışı"))
                .findFirst()
                .or(() -> categories.stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Income category not found"))
                .getId();
    }

    private Long resolveRevenueChannelId(Long companyId) {
        return revenueChannelRepository.findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(companyId).stream()
                .filter(channel -> channel.getName().equalsIgnoreCase("Bilet Platformu") || channel.getName().equalsIgnoreCase("Kapı Satışı"))
                .findFirst()
                .or(() -> revenueChannelRepository.findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(companyId).stream().findFirst())
                .map(RevenueChannel::getId)
                .orElse(null);
    }

    private Long resolveFinancePaymentMethodId(Long companyId, com.dada.eventmanagement.common.enums.PaymentMethod paymentMethod) {
        PaymentMethodType type = switch (paymentMethod) {
            case CASH -> PaymentMethodType.CASH;
            case CREDIT_CARD -> PaymentMethodType.CARD;
            case BANK_TRANSFER -> PaymentMethodType.BANK;
            case OTHER -> PaymentMethodType.OTHER;
        };
        return financePaymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, type)
                .or(() -> financePaymentMethodRepository.findFirstByCompanyIdAndMethodTypeAndIsActiveTrueOrderByIsDefaultDescIdAsc(companyId, PaymentMethodType.CASH))
                .map(PaymentMethod::getId)
                .orElse(null);
    }

    private String paymentDescription(Reservation reservation, ReservationPaymentRequest request) {
        String description = request.description() == null || request.description().isBlank()
                ? "Rezervasyon ödemesi"
                : request.description().trim();
        return description + " - Rezervasyon #" + reservation.getReservationCode();
    }
}
