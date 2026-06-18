package com.dada.eventmanagement.auth.dto;

import java.util.List;

public record OperationsReportPreferencesRequest(
        List<String> daily,
        List<String> events,
        List<String> totals
) {
}
