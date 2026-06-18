package com.dada.eventmanagement.auth.dto;

import java.util.List;

public record OperationsReportPreferencesResponse(
        List<String> daily,
        List<String> events,
        List<String> totals
) {
}
