package com.dada.eventmanagement.staff.dto;

import com.dada.eventmanagement.common.enums.EmployeeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record EmployeeRequest(
        @NotBlank(message = "Employee name is required") String fullName,
        String roleName,
        @NotNull(message = "Employee type is required") EmployeeType employeeType,
        BigDecimal defaultDailyRate,
        BigDecimal servicePoint,
        String phone,
        String notes
) {
}
