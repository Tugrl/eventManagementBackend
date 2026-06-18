package com.dada.eventmanagement.cost.service;

import com.dada.eventmanagement.common.exception.ResourceNotFoundException;
import com.dada.eventmanagement.common.util.SecurityUtils;
import com.dada.eventmanagement.cost.dto.CostCategoryRequest;
import com.dada.eventmanagement.cost.dto.CostCategoryResponse;
import com.dada.eventmanagement.cost.entity.CostCategory;
import com.dada.eventmanagement.cost.repository.CostCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CostCategoryService {
    private final CostCategoryRepository repository;

    public CostCategoryService(CostCategoryRepository repository) {
        this.repository = repository;
    }

    public List<CostCategoryResponse> list() {
        Long companyId = SecurityUtils.currentCompanyId();
        return repository.findByCompanyIdAndIsActiveTrueOrderByNameAsc(companyId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CostCategoryResponse create(CostCategoryRequest request) {
        CostCategory entity = new CostCategory();
        entity.setCompanyId(SecurityUtils.currentCompanyId());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setIsDefault(false);
        entity.setIsActive(true);
        return toResponse(repository.save(entity));
    }

    @Transactional
    public CostCategoryResponse update(Long id, CostCategoryRequest request) {
        CostCategory category = find(id);
        category.setName(request.name().trim());
        category.setDescription(request.description());
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        CostCategory category = find(id);
        category.setIsActive(false);
        repository.save(category);
    }

    public CostCategory find(Long id) {
        return repository.findByIdAndCompanyId(id, SecurityUtils.currentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Cost category not found"));
    }

    private CostCategoryResponse toResponse(CostCategory c) {
        return new CostCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getIsDefault(), c.getIsActive());
    }
}
