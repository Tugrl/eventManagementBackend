package com.dada.eventmanagement.finance.repository;

import com.dada.eventmanagement.finance.entity.RevenueChannel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueChannelRepository extends JpaRepository<RevenueChannel, Long> {
    List<RevenueChannel> findByCompanyIdAndIsActiveTrueOrderBySortOrderAscNameAsc(Long companyId);
    Optional<RevenueChannel> findByIdAndCompanyIdAndIsActiveTrue(Long id, Long companyId);
}
