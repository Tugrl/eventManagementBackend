package com.dada.eventmanagement.staff.dto;

import com.dada.eventmanagement.common.enums.EmployeeType;
import java.math.BigDecimal;

public record EmployeeResponse(
        Long id,
        String fullName,
        Long contactId,
        String contactName,
        String roleName,
        EmployeeType employeeType,
        BigDecimal defaultDailyRate,
        BigDecimal servicePoint,
        String phone,
        String notes
) {
}
