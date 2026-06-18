package com.dada.eventmanagement.staff.service;

import com.dada.eventmanagement.common.enums.EmployeeType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.VoidTransactionRequest;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.staff.dto.*;
import com.dada.eventmanagement.staff.entity.Employee;
import com.dada.eventmanagement.staff.entity.ServicePayout;
import com.dada.eventmanagement.staff.entity.ServicePayoutItem;
import com.dada.eventmanagement.staff.repository.EmployeeRepository;
import com.dada.eventmanagement.staff.repository.ServicePayoutItemRepository;
import com.dada.eventmanagement.staff.repository.ServicePayoutRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    private final EmployeeRepository employeeRepository;
    private final ServicePayoutRepository payoutRepository;
    private final ServicePayoutItemRepository itemRepository;
    private final EventRepository eventRepository;
    private final FinanceService financeService;

    public StaffService(EmployeeRepository employeeRepository, ServicePayoutRepository payoutRepository, ServicePayoutItemRepository itemRepository, EventRepository eventRepository, FinanceService financeService) {
        this.employeeRepository = employeeRepository;
        this.payoutRepository = payoutRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.financeService = financeService;
    }

    public List<EmployeeResponse> employees(EmployeeType type) {
        Long companyId = SecurityUtils.currentCompanyId();
        List<Employee> rows = type == null
                ? employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId)
                : employeeRepository.findByCompanyIdAndEmployeeTypeAndIsActiveTrueOrderByFullNameAsc(companyId, type);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setCompanyId(SecurityUtils.currentCompanyId());
        apply(employee, request);
        employee.setIsActive(true);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);
        apply(employee, request);
        return toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployee(id);
        employee.setIsActive(false);
        employeeRepository.save(employee);
    }

    @Transactional
    public ServicePayoutResponse createPayout(ServicePayoutRequest request) {
        return savePayout(new ServicePayout(), request);
    }

    @Transactional
    public ServicePayoutResponse updatePayout(Long id, ServicePayoutRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        ServicePayout payout = findPayout(id, companyId);
        voidLinkedTransaction(payout, "Servis hakedişi güncellendi");
        itemRepository.deleteByCompanyIdAndPayoutId(companyId, payout.getId());
        return savePayout(payout, request);
    }

    @Transactional
    public void deletePayout(Long id) {
        Long companyId = SecurityUtils.currentCompanyId();
        ServicePayout payout = findPayout(id, companyId);
        voidLinkedTransaction(payout, "Servis hakedişi silindi");
        itemRepository.deleteByCompanyIdAndPayoutId(companyId, payout.getId());
        payoutRepository.delete(payout);
    }

    private ServicePayoutResponse savePayout(ServicePayout payout, ServicePayoutRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        if (request.eventId() != null) {
            eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(request.eventId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        }
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        BigDecimal totalPoints = request.items().stream()
                .map(item -> points(item, employees))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Total points must be greater than zero");
        }
        BigDecimal amountPerPoint = request.totalServiceAmount().divide(totalPoints, 2, RoundingMode.HALF_UP);

        payout.setCompanyId(companyId);
        payout.setPayoutDate(request.payoutDate());
        payout.setEventId(request.eventId());
        payout.setTotalServiceAmount(request.totalServiceAmount());
        payout.setTotalPoints(totalPoints);
        payout.setAmountPerPoint(amountPerPoint);
        payout.setNotes(request.notes());
        if (payout.getCreatedByUserId() == null) {
            payout.setCreatedByUserId(SecurityUtils.currentUser().getId());
        }

        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                request.eventId(),
                request.accountId(),
                request.paymentMethodId(),
                request.categoryId(),
                null,
                null,
                FinancialTransactionType.EXPENSE,
                request.payoutDate(),
                request.totalServiceAmount(),
                null,
                description(request.notes())
        ));
        payout.setFinancialTransactionId(tx.getId());
        payout = payoutRepository.save(payout);

        for (ServicePayoutItemRequest itemRequest : request.items()) {
            Employee employee = employees.get(itemRequest.employeeId());
            if (employee == null) {
                throw new ResourceNotFoundException("Employee not found");
            }
            BigDecimal points = points(itemRequest, employees);
            ServicePayoutItem item = new ServicePayoutItem();
            item.setCompanyId(companyId);
            item.setPayoutId(payout.getId());
            item.setEmployeeId(employee.getId());
            item.setPoints(points);
            item.setPayoutAmount(points.multiply(amountPerPoint).setScale(2, RoundingMode.HALF_UP));
            itemRepository.save(item);
        }
        return toResponse(payout);
    }

    public List<ServicePayoutResponse> payouts(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return payoutRepository.findByCompanyIdAndPayoutDateBetweenOrderByPayoutDateDesc(companyId, start, end).stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal points(ServicePayoutItemRequest request, Map<Long, Employee> employees) {
        if (request.points() != null) {
            return request.points();
        }
        Employee employee = employees.get(request.employeeId());
        if (employee == null) {
            throw new ResourceNotFoundException("Employee not found");
        }
        return employee.getServicePoint();
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyIdAndIsActiveTrue(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private ServicePayout findPayout(Long id, Long companyId) {
        return payoutRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Service payout not found"));
    }

    private void voidLinkedTransaction(ServicePayout payout, String reason) {
        if (payout.getFinancialTransactionId() != null) {
            financeService.voidTransaction(payout.getFinancialTransactionId(), new VoidTransactionRequest(reason));
            payout.setFinancialTransactionId(null);
        }
    }

    private void apply(Employee employee, EmployeeRequest request) {
        employee.setFullName(request.fullName().trim());
        employee.setRoleName(request.roleName());
        employee.setEmployeeType(request.employeeType());
        employee.setDefaultDailyRate(nvl(request.defaultDailyRate()));
        employee.setServicePoint(nvl(request.servicePoint()));
        employee.setPhone(request.phone());
        employee.setNotes(request.notes());
    }

    private ServicePayoutResponse toResponse(ServicePayout payout) {
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(payout.getCompanyId()).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<ServicePayoutItemResponse> items = itemRepository.findByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId()).stream()
                .map(item -> new ServicePayoutItemResponse(
                        item.getEmployeeId(),
                        employees.containsKey(item.getEmployeeId()) ? employees.get(item.getEmployeeId()).getFullName() : "-",
                        item.getPoints(),
                        item.getPayoutAmount()
                ))
                .toList();
        return new ServicePayoutResponse(payout.getId(), payout.getPayoutDate(), payout.getEventId(), payout.getTotalServiceAmount(), payout.getTotalPoints(), payout.getAmountPerPoint(), payout.getFinancialTransactionId(), payout.getNotes(), items);
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getFullName(), employee.getRoleName(), employee.getEmployeeType(), employee.getDefaultDailyRate(), employee.getServicePoint(), employee.getPhone(), employee.getNotes());
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String description(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Servis hakedisi";
        }
        return "Servis hakedisi - " + notes.trim();
    }
}
