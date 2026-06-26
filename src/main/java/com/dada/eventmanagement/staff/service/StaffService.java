package com.dada.eventmanagement.staff.service;

import com.dada.eventmanagement.common.enums.EmployeeType;
import com.dada.eventmanagement.common.enums.FinancialTransactionType;
import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.enums.OperationContext;
import com.dada.eventmanagement.common.enums.OperationSource;
import com.dada.eventmanagement.common.enums.CalculationType;
import com.dada.eventmanagement.common.enums.CostPaymentStatus;
import com.dada.eventmanagement.common.enums.ServicePayoutClosingStatus;
import com.dada.eventmanagement.common.enums.ServicePayoutSource;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.cost.entity.CostCategory;
import com.dada.eventmanagement.cost.entity.EventCost;
import com.dada.eventmanagement.cost.repository.CostCategoryRepository;
import com.dada.eventmanagement.cost.repository.EventCostRepository;
import com.dada.eventmanagement.event.entity.Event;
import com.dada.eventmanagement.event.repository.EventRepository;
import com.dada.eventmanagement.contact.entity.Contact;
import com.dada.eventmanagement.contact.repository.ContactRepository;
import com.dada.eventmanagement.contact.service.ContactService;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.dto.ContactMovementResponse;
import com.dada.eventmanagement.finance.dto.FinancialTransactionRequest;
import com.dada.eventmanagement.finance.dto.VoidTransactionRequest;
import com.dada.eventmanagement.finance.dto.FinanceDocumentResponse;
import com.dada.eventmanagement.finance.dto.FinanceDocumentSettlementResponse;
import com.dada.eventmanagement.finance.entity.FinancialTransaction;
import com.dada.eventmanagement.finance.service.FinanceService;
import com.dada.eventmanagement.staff.dto.*;
import com.dada.eventmanagement.staff.entity.Employee;
import com.dada.eventmanagement.staff.entity.EventStaffAssignment;
import com.dada.eventmanagement.staff.entity.ServicePayout;
import com.dada.eventmanagement.staff.entity.ServicePayoutItem;
import com.dada.eventmanagement.staff.repository.EmployeeRepository;
import com.dada.eventmanagement.staff.repository.EventStaffAssignmentRepository;
import com.dada.eventmanagement.staff.repository.ServicePayoutItemRepository;
import com.dada.eventmanagement.staff.repository.ServicePayoutRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    private static final String STAFF_COST_PREFIX = "Personel - ";
    private static final String FIXED_TEAM_CATEGORY = "Sabit Personel";
    private static final String EXTRA_TEAM_CATEGORY = "Ek Personel";

    private final EmployeeRepository employeeRepository;
    private final EventStaffAssignmentRepository assignmentRepository;
    private final ServicePayoutRepository payoutRepository;
    private final ServicePayoutItemRepository itemRepository;
    private final EventRepository eventRepository;
    private final EventCostRepository eventCostRepository;
    private final CostCategoryRepository costCategoryRepository;
    private final ContactRepository contactRepository;
    private final ContactService contactService;
    private final FinanceService financeService;

    public StaffService(
            EmployeeRepository employeeRepository,
            EventStaffAssignmentRepository assignmentRepository,
            ServicePayoutRepository payoutRepository,
            ServicePayoutItemRepository itemRepository,
            EventRepository eventRepository,
            EventCostRepository eventCostRepository,
            CostCategoryRepository costCategoryRepository,
            ContactRepository contactRepository,
            ContactService contactService,
            FinanceService financeService
    ) {
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.payoutRepository = payoutRepository;
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
        this.eventCostRepository = eventCostRepository;
        this.costCategoryRepository = costCategoryRepository;
        this.contactRepository = contactRepository;
        this.contactService = contactService;
        this.financeService = financeService;
    }

    public List<EmployeeResponse> employees(EmployeeType type) {
        Long companyId = SecurityUtils.currentCompanyId();
        List<Employee> rows = type == null
                ? employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId)
                : employeeRepository.findByCompanyIdAndEmployeeTypeAndIsActiveTrueOrderByFullNameAsc(companyId, type);
        return mapEmployees(rows);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setCompanyId(SecurityUtils.currentCompanyId());
        apply(employee, request);
        employee.setContactId(resolveContactId(request.contactId()));
        employee.setIsActive(true);
        return toResponse(employeeRepository.save(employee), contactMap(List.of(employee)));
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = findEmployee(id);
        apply(employee, request);
        employee.setContactId(resolveContactId(request.contactId()));
        return toResponse(employeeRepository.save(employee), contactMap(List.of(employee)));
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = findEmployee(id);
        employee.setIsActive(false);
        employeeRepository.save(employee);
    }

    public List<EventStaffAssignmentResponse> eventAssignments(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return toAssignmentResponses(companyId, eventId);
    }

    @Transactional
    public List<EventStaffAssignmentResponse> saveEventAssignments(Long eventId, EventStaffAssignmentRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        Event event = eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        payoutRepository.findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .filter(payout -> payout.getClosingStatus() != ServicePayoutClosingStatus.DRAFT)
                .ifPresent(payout -> {
                    throw new BadRequestException("Finalized service payout prevents staff assignment changes");
                });
        List<EventStaffAssignmentItemRequest> items = request.items() == null ? List.of() : request.items();
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        Set<Long> uniqueEmployeeIds = new HashSet<>();
        for (EventStaffAssignmentItemRequest item : items) {
            if (!uniqueEmployeeIds.add(item.employeeId())) {
                throw new BadRequestException("Ayni personel bir etkinlige birden fazla kez eklenemez");
            }
            if (!employees.containsKey(item.employeeId())) {
                throw new ResourceNotFoundException("Employee not found");
            }
        }

        assignmentRepository.deleteByCompanyIdAndEventId(companyId, eventId);
        for (EventStaffAssignmentItemRequest item : items) {
            Employee employee = employees.get(item.employeeId());
            EventStaffAssignment assignment = new EventStaffAssignment();
            assignment.setCompanyId(companyId);
            assignment.setEventId(eventId);
            assignment.setEmployeeId(employee.getId());
            assignment.setPlannedDailyCost(item.plannedDailyCost() == null ? nvl(employee.getDefaultDailyRate()) : nvl(item.plannedDailyCost()));
            assignment.setServicePoint(item.servicePoint() == null ? nvl(employee.getServicePoint()) : nvl(item.servicePoint()));
            assignment.setNotes(item.notes());
            assignmentRepository.save(assignment);
        }

        syncAssignmentCosts(event, employees);
        event.setCostFinalized(false);
        event.setPricingFinalized(false);
        eventRepository.save(event);
        return toAssignmentResponses(companyId, eventId);
    }

    public int eventAssignmentCount(Long eventId) {
        return (int) assignmentRepository.countByCompanyIdAndEventId(SecurityUtils.currentCompanyId(), eventId);
    }

    public TeamBreakdown eventTeamBreakdown(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        int fixed = 0;
        int extra = 0;
        for (EventStaffAssignment assignment : assignmentRepository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(companyId, eventId)) {
            Employee employee = employees.get(assignment.getEmployeeId());
            if (employee == null) {
                continue;
            }
            if (employee.getEmployeeType() == EmployeeType.FIXED) {
                fixed++;
            } else {
                extra++;
            }
        }
        return new TeamBreakdown(fixed, extra);
    }

    @Transactional
    public ServicePayoutResponse createPayout(ServicePayoutRequest request) {
        if (request != null) {
            throw new BadRequestException("Manual service payouts are disabled; use event closing");
        }
        return savePayout(new ServicePayout(), request);
    }

    @Transactional
    public ServicePayoutResponse updatePayout(Long id, ServicePayoutRequest request) {
        if (id != null || request != null) {
            throw new BadRequestException("Manual service payouts are disabled; use event closing");
        }
        Long companyId = SecurityUtils.currentCompanyId();
        ServicePayout payout = findPayout(id, companyId);
        voidLinkedTransaction(payout, "Servis hakedişi güncellendi");
        itemRepository.deleteByCompanyIdAndPayoutId(companyId, payout.getId());
        return savePayout(payout, request);
    }

    @Transactional
    public void deletePayout(Long id) {
        if (id != null) {
            throw new BadRequestException("Service payout history cannot be deleted");
        }
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
        payout = payoutRepository.save(payout);

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
                description(request.notes()),
                OperationSource.STAFF_PAYOUT,
                request.eventId() != null ? OperationContext.EVENT : OperationContext.COMPANY,
                payout.getId()
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

    public ServicePayoutResponse closingServicePayout(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        Event event = eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return payoutRepository.findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .map(this::toResponse)
                .orElseGet(() -> closingPreview(event));
    }

    @Transactional
    public ServicePayoutResponse saveClosingServicePayout(Long eventId, ClosingServicePayoutRequest request) {
        Long companyId = SecurityUtils.currentCompanyId();
        Event event = eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        ServicePayout payout = payoutRepository
                .findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .orElseGet(ServicePayout::new);
        if (payout.getId() != null && payout.getClosingStatus() != ServicePayoutClosingStatus.DRAFT) {
            throw new BadRequestException("Finalized service payout cannot be changed");
        }
        validateClosingRequest(request);

        payout.setCompanyId(companyId);
        payout.setEventId(eventId);
        payout.setPayoutSource(ServicePayoutSource.EVENT_CLOSING);
        payout.setClosingStatus(ServicePayoutClosingStatus.DRAFT);
        payout.setPaymentStatus(request.paymentStatus());
        payout.setPayoutDate(request.payoutDate());
        payout.setTotalServiceAmount(money(request.totalServiceAmount()));
        payout.setAccountId(request.paymentStatus() == CostPaymentStatus.PAID ? request.accountId() : null);
        payout.setPaymentMethodId(request.paymentStatus() == CostPaymentStatus.PAID ? request.paymentMethodId() : null);
        payout.setCategoryId(request.categoryId());
        payout.setPaymentDate(request.paymentStatus() == CostPaymentStatus.PAID ? request.paymentDate() : null);
        payout.setNotes(request.notes());
        if (payout.getCreatedByUserId() == null) {
            payout.setCreatedByUserId(SecurityUtils.currentUser().getId());
        }

        List<EventStaffAssignment> eligible = eligibleAssignments(companyId, eventId);
        BigDecimal totalPoints = eligible.stream().map(EventStaffAssignment::getServicePoint).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (payout.getTotalServiceAmount().compareTo(BigDecimal.ZERO) > 0 && totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("At least one assigned employee must have service points");
        }
        payout.setTotalPoints(totalPoints);
        payout.setAmountPerPoint(totalPoints.compareTo(BigDecimal.ZERO) > 0
                ? payout.getTotalServiceAmount().divide(totalPoints, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        payout = payoutRepository.save(payout);
        rebuildClosingItems(payout, eligible);
        return toResponse(payout);
    }

    @Transactional
    public ServicePayoutResponse finalizeClosingServicePayout(Long eventId) {
        Event event = eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        ServicePayout payout = findClosingPayout(eventId);
        if (payout.getClosingStatus() != ServicePayoutClosingStatus.DRAFT) {
            return toResponse(payout);
        }
        if (payout.getTotalServiceAmount().compareTo(BigDecimal.ZERO) == 0) {
            itemRepository.deleteByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId());
            payout.setClosingStatus(ServicePayoutClosingStatus.SKIPPED);
            payout.setPaymentStatus(CostPaymentStatus.UNPAID);
        } else {
            List<EventStaffAssignment> eligible = eligibleAssignments(payout.getCompanyId(), eventId);
            BigDecimal totalPoints = eligible.stream().map(EventStaffAssignment::getServicePoint).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalPoints.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Service payout has no eligible employees");
            }
            payout.setTotalPoints(totalPoints);
            payout.setAmountPerPoint(payout.getTotalServiceAmount().divide(totalPoints, 2, RoundingMode.HALF_UP));
            payoutRepository.save(payout);
            rebuildClosingItems(payout, eligible);
            List<ServicePayoutItem> items = itemRepository.findByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId());
            if (items.isEmpty()) {
                throw new BadRequestException("Service payout has no eligible employees");
            }
            if (payout.getCategoryId() == null) {
                throw new BadRequestException("Financial category is required for service payout");
            }
            validateClosingItemContacts(payout, items);
            createClosingPayableDocuments(payout, items, event);
            if (payout.getPaymentStatus() == CostPaymentStatus.PAID) {
                postClosingPayout(payout);
            }
            payout.setClosingStatus(ServicePayoutClosingStatus.FINALIZED);
        }
        return toResponse(payoutRepository.save(payout));
    }

    @Transactional
    public ServicePayoutResponse payClosingServicePayout(Long eventId, ClosingServicePayoutPaymentRequest request) {
        ServicePayout payout = findClosingPayout(eventId);
        if (payout.getClosingStatus() != ServicePayoutClosingStatus.FINALIZED) {
            throw new BadRequestException("Only finalized service payouts can be paid");
        }
        if (payout.getPaymentStatus() == CostPaymentStatus.PAID) {
            return toResponse(payout);
        }
        List<ServicePayoutItem> items = itemRepository.findByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("Service payout has no eligible employees");
        }
        if (payout.getCategoryId() == null) {
            throw new BadRequestException("Financial category is required for service payout");
        }
        payout.setAccountId(request.accountId());
        payout.setPaymentMethodId(request.paymentMethodId());
        payout.setCategoryId(request.categoryId());
        payout.setPaymentDate(request.paymentDate());
        payout.setPaymentStatus(CostPaymentStatus.PAID);
        payClosingPayables(payout, items, request);
        return toResponse(payoutRepository.save(payout));
    }

    public void validateClosingPayoutFinalized(Long eventId) {
        ServicePayout payout = findClosingPayout(eventId);
        if (payout.getClosingStatus() == ServicePayoutClosingStatus.DRAFT) {
            throw new BadRequestException("Event service payout must be finalized before creating the report");
        }
    }

    public BigDecimal totalFinalizedServiceCost(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        return payoutRepository.findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .filter(payout -> payout.getClosingStatus() == ServicePayoutClosingStatus.FINALIZED)
                .map(ServicePayout::getTotalServiceAmount)
                .map(this::money)
                .orElse(BigDecimal.ZERO);
    }

    public List<ServicePayoutResponse> payouts(LocalDate startDate, LocalDate endDate) {
        Long companyId = SecurityUtils.currentCompanyId();
        LocalDate start = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        return payoutRepository.findByCompanyIdAndPayoutDateBetweenOrderByPayoutDateDesc(companyId, start, end).stream()
                .filter(payout -> payout.getClosingStatus() != ServicePayoutClosingStatus.DRAFT)
                .map(this::toResponse)
                .toList();
    }

    private ServicePayoutResponse closingPreview(Event event) {
        List<EventStaffAssignment> assignments = eligibleAssignments(event.getCompanyId(), event.getId());
        Map<Long, Employee> employees = activeEmployeeMap(event.getCompanyId());
        Map<Long, Contact> contacts = contactMap(new ArrayList<>(employees.values()));
        BigDecimal totalPoints = assignments.stream().map(EventStaffAssignment::getServicePoint).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ServicePayoutItemResponse> items = assignments.stream()
                .map(row -> {
                    Employee employee = employees.get(row.getEmployeeId());
                    Contact contact = employee == null || employee.getContactId() == null ? null : contacts.get(employee.getContactId());
                    return new ServicePayoutItemResponse(
                            row.getEmployeeId(),
                            employeeName(employees, row.getEmployeeId()),
                            row.getServicePoint(),
                            BigDecimal.ZERO,
                            employee == null ? null : employee.getContactId(),
                            contact == null ? null : contact.getName(),
                            null,
                            null,
                            false,
                            null,
                            null,
                            null,
                            null,
                            null
                    );
                })
                .toList();
        return new ServicePayoutResponse(null, event.getEventDate(), event.getId(), ServicePayoutSource.EVENT_CLOSING,
                ServicePayoutClosingStatus.DRAFT, CostPaymentStatus.UNPAID, BigDecimal.ZERO, totalPoints,
                BigDecimal.ZERO, null, null, null, null, null, null, items);
    }

    private void validateClosingRequest(ClosingServicePayoutRequest request) {
        if (request.paymentStatus() == CostPaymentStatus.STOCK) {
            throw new BadRequestException("Service payout payment status must be PAID or UNPAID");
        }
        if (request.totalServiceAmount().compareTo(BigDecimal.ZERO) > 0 && request.categoryId() == null) {
            throw new BadRequestException("Financial category is required for service payout");
        }
        if (request.totalServiceAmount().compareTo(BigDecimal.ZERO) > 0 && request.paymentStatus() == CostPaymentStatus.PAID
                && (request.accountId() == null || request.paymentDate() == null)) {
            throw new BadRequestException("Paid service payout requires account and payment date");
        }
    }

    private ServicePayout findClosingPayout(Long eventId) {
        Long companyId = SecurityUtils.currentCompanyId();
        eventRepository.findByIdAndCompanyIdAndIsDeletedFalse(eventId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return payoutRepository.findByCompanyIdAndEventIdAndPayoutSource(companyId, eventId, ServicePayoutSource.EVENT_CLOSING)
                .orElseThrow(() -> new BadRequestException("Save the service payout before finalizing it"));
    }

    private List<EventStaffAssignment> eligibleAssignments(Long companyId, Long eventId) {
        return assignmentRepository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(companyId, eventId).stream()
                .filter(row -> nvl(row.getServicePoint()).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    private void rebuildClosingItems(ServicePayout payout, List<EventStaffAssignment> assignments) {
        itemRepository.deleteByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId());
        if (payout.getTotalServiceAmount().compareTo(BigDecimal.ZERO) == 0 || assignments.isEmpty()) {
            return;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < assignments.size(); index++) {
            EventStaffAssignment assignment = assignments.get(index);
            BigDecimal amount = index == assignments.size() - 1
                    ? payout.getTotalServiceAmount().subtract(allocated)
                    : payout.getTotalServiceAmount().multiply(assignment.getServicePoint()).divide(payout.getTotalPoints(), 2, RoundingMode.DOWN);
            amount = money(amount);
            allocated = allocated.add(amount);
            ServicePayoutItem item = new ServicePayoutItem();
            item.setCompanyId(payout.getCompanyId());
            item.setPayoutId(payout.getId());
            item.setEmployeeId(assignment.getEmployeeId());
            item.setPoints(assignment.getServicePoint());
            item.setPayoutAmount(amount);
            itemRepository.save(item);
        }
    }

    private void validateClosingItemContacts(ServicePayout payout, List<ServicePayoutItem> items) {
        List<ServicePayoutItem> payableItems = items.stream()
                .filter(item -> item.getPayoutAmount() != null && item.getPayoutAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        if (payableItems.isEmpty()) {
            return;
        }

        Long companyId = payout.getCompanyId();
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIdIn(
                        companyId,
                        payableItems.stream().map(ServicePayoutItem::getEmployeeId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        List<Long> contactIds = employees.values().stream()
                .map(Employee::getContactId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Contact> contacts = contactIds.isEmpty()
                ? Map.of()
                : contactRepository.findByCompanyIdAndIdInAndIsActiveTrue(companyId, contactIds).stream()
                .collect(Collectors.toMap(Contact::getId, Function.identity()));

        List<String> missingNames = new ArrayList<>();
        for (ServicePayoutItem item : payableItems) {
            Employee employee = employees.get(item.getEmployeeId());
            if (employee == null) {
                missingNames.add("Employee #" + item.getEmployeeId());
                continue;
            }
            Long contactId = employee.getContactId();
            if (contactId == null || !contacts.containsKey(contactId)) {
                missingNames.add(employee.getFullName());
            }
        }

        if (!missingNames.isEmpty()) {
            throw new BadRequestException("Servis hakedişi finalize edilemez. Cari bağlantısı eksik personeller: " + String.join(", ", missingNames));
        }
    }

    private void createClosingPayableDocuments(ServicePayout payout, List<ServicePayoutItem> items, Event event) {
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIdIn(
                        payout.getCompanyId(),
                        items.stream().map(ServicePayoutItem::getEmployeeId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, Contact> contacts = contactMap(new ArrayList<>(employees.values()));

        for (ServicePayoutItem item : items) {
            if (item.getPayoutAmount() == null || item.getPayoutAmount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (item.getFinanceDocumentId() != null) {
                continue;
            }

            Employee employee = employees.get(item.getEmployeeId());
            if (employee == null || employee.getContactId() == null) {
                throw new BadRequestException("Servis hakedişi finalize edilemez. Cari bağlantısı eksik personeller: " + employeeLabel(employee, item.getEmployeeId()));
            }
            Contact contact = contacts.get(employee.getContactId());
            if (contact == null) {
                throw new BadRequestException("Servis hakedişi finalize edilemez. Cari bağlantısı eksik personeller: " + employee.getFullName());
            }

            ContactMovementResponse movement = contactService.createMovement(contact.getId(), new ContactMovementRequest(
                    payout.getPayoutDate(),
                    com.dada.eventmanagement.common.enums.ContactMovementType.PAYABLE,
                    item.getPayoutAmount(),
                    null,
                    null,
                    null,
                    payout.getCategoryId(),
                    null,
                    payout.getEventId(),
                    null,
                    null,
                    servicePayoutDescription(event, employee, payout)
            ));
            item.setContactMovementId(movement.id());
            item.setFinanceDocumentId(movement.documentId());
            itemRepository.save(item);
        }
    }

    private void payClosingPayables(ServicePayout payout, List<ServicePayoutItem> items, ClosingServicePayoutPaymentRequest request) {
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIdIn(
                        payout.getCompanyId(),
                        items.stream().map(ServicePayoutItem::getEmployeeId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));

        for (ServicePayoutItem item : items) {
            if (item.getFinanceDocumentId() == null || item.getContactMovementId() == null) {
                throw new BadRequestException("Bu hakediş için cari belge bağlantısı yok. Hakediş belge modeline taşınmadan ödeme yapılamaz.");
            }
            Employee employee = employees.get(item.getEmployeeId());
            if (employee == null || employee.getContactId() == null) {
                throw new BadRequestException("Bu hakediş için cari belge bağlantısı yok. Hakediş belge modeline taşınmadan ödeme yapılamaz.");
            }

            FinanceDocumentResponse document = financeService.documentDetail(item.getFinanceDocumentId());
            if (document.documentType() != FinanceDocumentType.EXPENSE) {
                throw new BadRequestException("Only EXPENSE documents can be paid");
            }
            if (document.status() == FinanceDocumentStatus.VOIDED || document.status() == FinanceDocumentStatus.SETTLED) {
                throw new BadRequestException("Payment cannot be created for settled or voided documents");
            }
            if (!employee.getContactId().equals(document.contactId())) {
                throw new BadRequestException("Document belongs to another contact");
            }
            if (document.remainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Payment cannot be created for settled or voided documents");
            }

            BigDecimal paymentAmount = document.remainingAmount();
            ContactMovementResponse movement = contactService.createMovement(employee.getContactId(), new ContactMovementRequest(
                    request.paymentDate(),
                    com.dada.eventmanagement.common.enums.ContactMovementType.PAYMENT,
                    paymentAmount,
                    null,
                    request.accountId(),
                    request.paymentMethodId(),
                    request.categoryId(),
                    null,
                    payout.getEventId(),
                    item.getFinanceDocumentId(),
                    null,
                    servicePayoutPaymentDescription(payout, employee, paymentAmount)
            ));
            if (movement.settlementId() == null || movement.financialTransactionId() == null) {
                throw new BadRequestException("Payment settlement could not be created");
            }
        }
    }

    private void postClosingPayout(ServicePayout payout) {
        if (payout.getFinancialTransactionId() != null) {
            return;
        }
        if (payout.getAccountId() == null || payout.getCategoryId() == null || payout.getPaymentDate() == null) {
            throw new BadRequestException("Paid service payout requires account, category and payment date");
        }
        FinancialTransaction tx = financeService.createTransactionEntity(new FinancialTransactionRequest(
                payout.getEventId(), payout.getAccountId(), payout.getPaymentMethodId(), payout.getCategoryId(),
                null, null, FinancialTransactionType.EXPENSE, payout.getPaymentDate(), payout.getTotalServiceAmount(),
                null, description(payout.getNotes()), OperationSource.STAFF_PAYOUT, OperationContext.EVENT, payout.getId()
        ));
        payout.setFinancialTransactionId(tx.getId());
    }

    private Map<Long, Employee> activeEmployeeMap(Long companyId) {
        return employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
    }

    private String employeeName(Map<Long, Employee> employees, Long employeeId) {
        Employee employee = employees.get(employeeId);
        return employee == null ? "-" : employee.getFullName();
    }

    private BigDecimal money(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
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
        employee.setContactId(request.contactId());
        employee.setRoleName(request.roleName());
        employee.setEmployeeType(request.employeeType());
        employee.setDefaultDailyRate(nvl(request.defaultDailyRate()));
        employee.setServicePoint(nvl(request.servicePoint()));
        employee.setPhone(request.phone());
        employee.setNotes(request.notes());
    }

    private ServicePayoutResponse toResponse(ServicePayout payout) {
        List<ServicePayoutItem> rows = itemRepository.findByCompanyIdAndPayoutId(payout.getCompanyId(), payout.getId());
        Map<Long, Employee> employees = rows.isEmpty()
                ? Map.of()
                : employeeRepository.findByCompanyIdAndIdIn(
                        payout.getCompanyId(),
                        rows.stream().map(ServicePayoutItem::getEmployeeId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        Map<Long, Contact> contacts = contactMap(new ArrayList<>(employees.values()));
        Map<Long, FinanceDocumentResponse> documents = rows.stream()
                .map(ServicePayoutItem::getFinanceDocumentId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), financeService::documentDetail));
        List<ServicePayoutItemResponse> items = rows.stream()
                .map(item -> {
                    Employee employee = employees.get(item.getEmployeeId());
                    Contact contact = employee == null || employee.getContactId() == null ? null : contacts.get(employee.getContactId());
                    FinanceDocumentResponse document = item.getFinanceDocumentId() == null ? null : documents.get(item.getFinanceDocumentId());
                    FinanceDocumentSettlementResponse latestSettlement = document == null || document.settlements() == null || document.settlements().isEmpty()
                            ? null
                            : document.settlements().stream()
                            .filter(settlement -> settlement.financialTransactionStatus() != com.dada.eventmanagement.common.enums.FinancialTransactionStatus.VOIDED)
                            .findFirst()
                            .orElse(null);
                    return new ServicePayoutItemResponse(
                            item.getEmployeeId(),
                            employee == null ? "-" : employee.getFullName(),
                            item.getPoints(),
                            item.getPayoutAmount(),
                            employee == null ? null : employee.getContactId(),
                            contact == null ? null : contact.getName(),
                            item.getContactMovementId(),
                            item.getFinanceDocumentId(),
                            item.getFinanceDocumentId() != null,
                            document == null ? null : document.settledAmount(),
                            document == null ? null : document.remainingAmount(),
                            document == null || document.status() == null ? null : document.status().name(),
                            latestSettlement == null ? null : latestSettlement.id(),
                            latestSettlement == null ? null : latestSettlement.financialTransactionId()
                    );
                })
                .toList();
        return new ServicePayoutResponse(
                payout.getId(), payout.getPayoutDate(), payout.getEventId(), payout.getPayoutSource(),
                payout.getClosingStatus(), payout.getPaymentStatus(), payout.getTotalServiceAmount(),
                payout.getTotalPoints(), payout.getAmountPerPoint(), payout.getFinancialTransactionId(),
                payout.getAccountId(), payout.getPaymentMethodId(), payout.getCategoryId(), payout.getPaymentDate(),
                payout.getNotes(), items
        );
    }

    private List<EmployeeResponse> mapEmployees(List<Employee> employees) {
        Map<Long, Contact> contacts = contactMap(employees);
        return employees.stream()
                .map(employee -> toResponse(employee, contacts))
                .toList();
    }

    private EmployeeResponse toResponse(Employee employee, Map<Long, Contact> contacts) {
        Contact contact = employee.getContactId() == null ? null : contacts.get(employee.getContactId());
        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getContactId(),
                contact == null ? null : contact.getName(),
                employee.getRoleName(),
                employee.getEmployeeType(),
                employee.getDefaultDailyRate(),
                employee.getServicePoint(),
                employee.getPhone(),
                employee.getNotes()
        );
    }

    private Map<Long, Contact> contactMap(List<Employee> employees) {
        Long companyId = SecurityUtils.currentCompanyId();
        List<Long> contactIds = employees.stream()
                .map(Employee::getContactId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (contactIds.isEmpty()) {
            return Map.of();
        }
        return contactRepository.findByCompanyIdAndIdInAndIsActiveTrue(companyId, contactIds).stream()
                .collect(Collectors.toMap(Contact::getId, Function.identity()));
    }

    private Long resolveContactId(Long contactId) {
        if (contactId == null) {
            return null;
        }
        Long companyId = SecurityUtils.currentCompanyId();
        Contact contact = contactRepository.findByIdAndCompanyIdAndIsActiveTrue(contactId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        return contact.getId();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<EventStaffAssignmentResponse> toAssignmentResponses(Long companyId, Long eventId) {
        Map<Long, Employee> employees = employeeRepository.findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(companyId).stream()
                .collect(Collectors.toMap(Employee::getId, Function.identity()));
        return assignmentRepository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(companyId, eventId).stream()
                .map(assignment -> {
                    Employee employee = employees.get(assignment.getEmployeeId());
                    if (employee == null) {
                        throw new ResourceNotFoundException("Employee not found");
                    }
                    return new EventStaffAssignmentResponse(
                            assignment.getId(),
                            assignment.getEventId(),
                            assignment.getEmployeeId(),
                            employee.getFullName(),
                            employee.getRoleName(),
                            employee.getEmployeeType(),
                            assignment.getPlannedDailyCost(),
                            assignment.getServicePoint(),
                            assignment.getNotes()
                    );
                })
                .toList();
    }

    private void syncAssignmentCosts(Event event, Map<Long, Employee> employees) {
        eventCostRepository.findByCompanyIdAndEventIdAndNameStartingWith(event.getCompanyId(), event.getId(), STAFF_COST_PREFIX)
                .forEach(eventCostRepository::delete);

        List<EventStaffAssignment> assignments = assignmentRepository.findByCompanyIdAndEventIdOrderByCreatedAtAsc(event.getCompanyId(), event.getId());
        for (EventStaffAssignment assignment : assignments) {
            Employee employee = employees.get(assignment.getEmployeeId());
            if (employee == null) {
                continue;
            }
            EventCost cost = new EventCost();
            cost.setCompanyId(event.getCompanyId());
            cost.setEventId(event.getId());
            cost.setCostCategoryId(resolveStaffCategoryId(event.getCompanyId(), employee.getEmployeeType()));
            cost.setName(STAFF_COST_PREFIX + employee.getFullName());
            cost.setDescription("Etkinlik ekibinden otomatik olusturuldu");
            cost.setCalculationType(CalculationType.PER_STAFF);
            cost.setUnitCost(nvl(assignment.getPlannedDailyCost()));
            cost.setQuantity(BigDecimal.ONE);
            cost.setTotalCost(nvl(assignment.getPlannedDailyCost()));
            cost.setIsEstimated(true);
            eventCostRepository.save(cost);
        }
    }

    private Long resolveStaffCategoryId(Long companyId, EmployeeType employeeType) {
        String categoryName = employeeType == EmployeeType.FIXED ? FIXED_TEAM_CATEGORY : EXTRA_TEAM_CATEGORY;
        return costCategoryRepository.findByCompanyIdAndNameIgnoreCase(companyId, categoryName)
                .or(() -> ensureStaffCategory(companyId, categoryName))
                .or(() -> costCategoryRepository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Cost category not found"))
                .getId();
    }

    private Optional<CostCategory> ensureStaffCategory(Long companyId, String categoryName) {
        CostCategory category = new CostCategory();
        category.setCompanyId(companyId);
        category.setName(categoryName);
        category.setDescription("Etkinlik ekip maliyetleri");
        category.setIsDefault(false);
        category.setIsActive(true);
        return Optional.of(costCategoryRepository.save(category));
    }

    private String description(String notes) {
        if (notes == null || notes.isBlank()) {
            return "Servis hakedisi";
        }
        return "Servis hakedisi - " + notes.trim();
    }

    private String servicePayoutDescription(Event event, Employee employee, ServicePayout payout) {
        String eventTitle = event.getTitle() == null || event.getTitle().isBlank() ? "Etkinlik" : event.getTitle().trim();
        String employeeName = employee == null ? "Personel" : employee.getFullName();
        String notes = payout.getNotes() == null || payout.getNotes().isBlank() ? "" : " - " + payout.getNotes().trim();
        return eventTitle + " - Servis hakedişi - " + employeeName + notes;
    }

    private String servicePayoutPaymentDescription(ServicePayout payout, Employee employee, BigDecimal paymentAmount) {
        String eventPart = payout.getEventId() == null ? "Servis hakedişi" : "Etkinlik #" + payout.getEventId();
        String employeeName = employee == null ? "Personel" : employee.getFullName();
        return eventPart + " - ödeme - " + employeeName + " - " + paymentAmount;
    }

    private String employeeLabel(Employee employee, Long employeeId) {
        return employee == null ? "Employee #" + employeeId : employee.getFullName();
    }

    public record TeamBreakdown(int fixedCount, int extraCount) {
    }
}
