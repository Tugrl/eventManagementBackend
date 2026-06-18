package com.dada.eventmanagement.company.repository;

import com.dada.eventmanagement.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
