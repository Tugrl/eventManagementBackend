package com.dada.eventmanagement.contact.controller;

import com.dada.eventmanagement.common.enums.ContactType;
import com.dada.eventmanagement.common.enums.FinanceDocumentStatus;
import com.dada.eventmanagement.common.enums.FinanceDocumentType;
import com.dada.eventmanagement.common.response.ApiResponse;
import com.dada.eventmanagement.contact.dto.ContactMovementRequest;
import com.dada.eventmanagement.contact.dto.ContactRequest;
import com.dada.eventmanagement.contact.service.ContactService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private final ContactService service;

    public ContactController(ContactService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) ContactType type) {
        return ApiResponse.ok("Contacts listed", service.list(type));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> create(@Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok("Contact created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok("Contact updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Contact deleted");
    }

    @GetMapping("/{id}/movements")
    public ApiResponse<?> movements(@PathVariable Long id) {
        return ApiResponse.ok("Contact movements listed", service.movements(id));
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<?> documents(
            @PathVariable Long id,
            @RequestParam(required = false) FinanceDocumentType documentType,
            @RequestParam(required = false) FinanceDocumentStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return ApiResponse.ok("Contact documents listed", service.documents(id, documentType, status, startDate, endDate));
    }

    @PostMapping("/{id}/movements")
    public ApiResponse<?> createMovement(@PathVariable Long id, @Valid @RequestBody ContactMovementRequest request) {
        return ApiResponse.ok("Contact movement created", service.createMovement(id, request));
    }

    @DeleteMapping("/{id}/movements/{movementId}")
    public ApiResponse<?> deleteMovement(@PathVariable Long id, @PathVariable Long movementId, @RequestParam String reason) {
        service.deleteMovement(id, movementId, reason);
        return ApiResponse.ok("Contact movement deleted");
    }
}
