package com.dada.eventmanagement.staff.repository;

import com.dada.eventmanagement.common.enums.EmployeeType;
import com.dada.eventmanagement.staff.entity.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByCompanyIdAndIsActiveTrueOrderByFullNameAsc(Long companyId);
    List<Employee> findByCompanyIdAndEmployeeTypeAndIsActiveTrueOrderByFullNameAsc(Long companyId, EmployeeType employeeType);
    Optional<Employee> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
}
