package com.dada.eventmanagement.staff.dto;

import jakarta.validation.Valid;
import java.util.List;

public record EventStaffAssignmentRequest(
        List<@Valid EventStaffAssignmentItemRequest> items
) {
}
