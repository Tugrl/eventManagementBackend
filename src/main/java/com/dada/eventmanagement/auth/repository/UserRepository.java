package com.dada.eventmanagement.auth.repository;

import com.dada.eventmanagement.auth.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailAndIsActiveTrue(String email);
    Optional<User> findByIdAndCompanyId(Long id, Long companyId);
    List<User> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
}
