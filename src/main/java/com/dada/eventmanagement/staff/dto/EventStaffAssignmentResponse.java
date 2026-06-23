package com.dada.eventmanagement.staff.dto;

import com.dada.eventmanagement.common.enums.EmployeeType;
import java.math.BigDecimal;

public record EventStaffAssignmentResponse(
        Long id,
        Long eventId,
        Long employeeId,
        String employeeName,
        String roleName,
        EmployeeType employeeType,
        BigDecimal plannedDailyCost,
        BigDecimal servicePoint,
        String notes
) {
}
